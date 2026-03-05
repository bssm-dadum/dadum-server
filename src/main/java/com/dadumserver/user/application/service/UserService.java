package com.dadumserver.user.application.service;

import com.dadumserver.common.util.JwtTokenProvider;
import com.dadumserver.user.application.dto.LoginCommand;
import com.dadumserver.user.application.dto.LoginResult;
import com.dadumserver.user.application.dto.RefreshCommand;
import com.dadumserver.user.domain.model.User;
import com.dadumserver.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

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

    if (user.getRefreshToken() == null
        || user.getRefreshTokenExpiresAt() == null
        || user.getRefreshTokenExpiresAt().isBefore(Instant.now())
        || !passwordEncoder.matches(refreshToken, user.getRefreshToken())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }

    return issueTokens(user);
  }

  private LoginResult issueTokens(User user) {
    String accessToken = jwtTokenProvider.generateAccessToken(user);
    String refreshToken = jwtTokenProvider.generateRefreshToken(user);

    user.updateRefreshToken(
        passwordEncoder.encode(refreshToken),
        Instant.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds())
    );
    userRepository.save(user);

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
}
