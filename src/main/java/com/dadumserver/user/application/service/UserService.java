package com.dadumserver.user.application.service;

import com.dadumserver.common.util.JwtTokenProvider;
import com.dadumserver.user.application.dto.LoginCommand;
import com.dadumserver.user.application.dto.LoginResult;
import com.dadumserver.user.application.dto.RefreshCommand;
import com.dadumserver.user.domain.model.BlackList;
import com.dadumserver.user.domain.model.User;
import com.dadumserver.user.infrastructure.persistence.BlackListRepository;
import com.dadumserver.user.infrastructure.persistence.RefreshTokenStore;
import com.dadumserver.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
      Pattern.CASE_INSENSITIVE
  );

  private final UserRepository userRepository;
  private final BlackListRepository blackListRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenStore refreshTokenStore;

  @Transactional
  public LoginResult login(LoginCommand command) {
    if (isBlank(command.email()) || isBlank(command.password())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
    }

    User user = userRepository.findByEmail(command.email())
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Invalid email or password"
        ));

    if (!passwordEncoder.matches(command.password(), user.getPassword())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    return issueTokens(user);
  }

  @Transactional
  public LoginResult refresh(RefreshCommand command) {
    if (isBlank(command.refreshToken())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required");
    }

    String refreshToken = command.refreshToken();
    if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }

    UUID userId = jwtTokenProvider.getUserId(refreshToken);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

    String storedRefreshToken = refreshTokenStore.findByUserId(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

    if (!passwordEncoder.matches(hashRefreshToken(refreshToken), storedRefreshToken)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }

    return issueTokens(user);
  }

  public User getUser(UUID userId, UUID requesterId) {
    return findUserById(userId);
  }

  public List<User> getUsers() {
    return userRepository.findAll();
  }

  @Transactional
  public User createUser(String email, String rawPassword) {
    String normalizedEmail = normalizeEmail(email);
    String normalizedPassword = normalizePassword(rawPassword);
    String emailHash = hashEmail(normalizedEmail);

    if (blackListRepository.existsByUserEmailHashed(emailHash)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Signup is blocked for this email");
    }

    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }

    User user = new User(
        null,
        normalizedEmail,
        passwordEncoder.encode(normalizedPassword)
    );

    try {
      return userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }
  }

  @Transactional
  public User updateUser(UUID userId, UUID requesterId, String email, String rawPassword) {
    User user = findUserById(userId);

    String nextEmail = null;
    if (!isBlank(email)) {
      nextEmail = normalizeEmail(email);
      if (!nextEmail.equals(user.getEmail()) && userRepository.existsByEmail(nextEmail)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
      }
    }

    String nextPassword = null;
    if (!isBlank(rawPassword)) {
      nextPassword = passwordEncoder.encode(normalizePassword(rawPassword));
    }

    if (nextEmail == null && nextPassword == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nothing to update");
    }

    user.updateProfile(nextEmail, nextPassword);

    try {
      return userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }
  }

  @Transactional
  public void deleteUser(UUID userId, UUID requesterId) {
    User user = findUserById(userId);
    refreshTokenStore.delete(userId);
    userRepository.delete(user);
  }

  @Transactional
  public void blacklistUser(UUID userId, UUID requesterId) {
    User user = findUserById(userId);
    String emailHash = hashEmail(user.getEmail());

    if (!blackListRepository.existsByUserEmailHashed(emailHash)) {
      blackListRepository.save(new BlackList(emailHash, Instant.now()));
    }

    refreshTokenStore.delete(userId);
  }

  private LoginResult issueTokens(User user) {
    String accessToken = jwtTokenProvider.generateAccessToken(user);
    String refreshToken = jwtTokenProvider.generateRefreshToken(user);

    refreshTokenStore.save(
        user.getId(),
        passwordEncoder.encode(hashRefreshToken(refreshToken)),
        Duration.ofSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds())
    );

    return new LoginResult(
        accessToken,
        jwtTokenProvider.getAccessTokenExpirationSeconds(),
        refreshToken,
        jwtTokenProvider.getRefreshTokenExpirationSeconds()
    );
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private User findUserById(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }

  private String normalizeEmail(String email) {
    if (isBlank(email) || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid email is required");
    }
    return email.trim().toLowerCase();
  }

  private String normalizePassword(String password) {
    if (isBlank(password)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
    }
    return password.trim();
  }

  private String hashEmail(String email) {
    return sha256Hex(email, "Unable to hash email");
  }

  private String hashRefreshToken(String refreshToken) {
    return sha256Hex(refreshToken, "Unable to hash refresh token");
  }

  private String sha256Hex(String value, String errorMessage) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder();
      for (byte current : hashed) {
        builder.append(String.format("%02x", current));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(errorMessage, exception);
    }
  }
}
