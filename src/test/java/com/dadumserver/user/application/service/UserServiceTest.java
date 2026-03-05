package com.dadumserver.user.application.service;

import com.dadumserver.common.util.JwtTokenProvider;
import com.dadumserver.user.application.dto.LoginCommand;
import com.dadumserver.user.application.dto.LoginResult;
import com.dadumserver.user.application.dto.RefreshCommand;
import com.dadumserver.user.domain.model.User;
import com.dadumserver.user.infrastructure.persistence.BlackListRepository;
import com.dadumserver.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
  @Mock
  private UserRepository userRepository;
  @Mock
  private BlackListRepository blackListRepository;
  @Mock
  private JwtTokenProvider jwtTokenProvider;

  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, blackListRepository, passwordEncoder, jwtTokenProvider);
  }

  @Test
  void loginReturnsAccessAndRefreshTokensWhenCredentialsAreValid() {
    String rawPassword = "password1234";
    User user = new User(
        UUID.randomUUID(),
        "kim@example.com",
        passwordEncoder.encode(rawPassword)
    );

    when(userRepository.findByEmail("kim@example.com")).thenReturn(Optional.of(user));
    when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access-token");
    when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");
    when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600L);
    when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(1209600L);

    LoginResult result = userService.login(new LoginCommand("kim@example.com", rawPassword));

    assertEquals("access-token", result.accessToken());
    assertEquals(3600L, result.accessTokenExpiresIn());
    assertEquals("refresh-token", result.refreshToken());
    assertEquals(1209600L, result.refreshTokenExpiresIn());
    verify(userRepository).save(user);
  }

  @Test
  void loginThrowsUnauthorizedWhenPasswordIsInvalid() {
    User user = new User(
        UUID.randomUUID(),
        "kim@example.com",
        passwordEncoder.encode("password1234")
    );

    when(userRepository.findByEmail("kim@example.com")).thenReturn(Optional.of(user));

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> userService.login(new LoginCommand("kim@example.com", "wrong-password"))
    );

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    verify(jwtTokenProvider, never()).generateAccessToken(user);
    verify(jwtTokenProvider, never()).generateRefreshToken(user);
  }

  @Test
  void refreshReturnsNewTokensWhenRefreshTokenIsValid() {
    UUID userId = UUID.randomUUID();
    User user = new User(
        userId,
        "kim@example.com",
        passwordEncoder.encode("password1234")
    );
    String refreshToken = "valid-refresh-token";
    user.updateRefreshToken(
        passwordEncoder.encode(refreshToken),
        Instant.now().plusSeconds(1000)
    );

    when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(true);
    when(jwtTokenProvider.getUserId(refreshToken)).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(jwtTokenProvider.generateAccessToken(user)).thenReturn("new-access-token");
    when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("new-refresh-token");
    when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600L);
    when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(1209600L);

    LoginResult result = userService.refresh(new RefreshCommand(refreshToken));

    assertEquals("new-access-token", result.accessToken());
    assertEquals("new-refresh-token", result.refreshToken());
    verify(userRepository).save(user);
  }

  @Test
  void refreshThrowsUnauthorizedWhenRefreshTokenIsInvalid() {
    String invalidRefreshToken = "invalid-refresh-token";
    when(jwtTokenProvider.validateRefreshToken(invalidRefreshToken)).thenReturn(false);

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> userService.refresh(new RefreshCommand(invalidRefreshToken))
    );

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void createUserThrowsForbiddenWhenEmailIsBlacklisted() {
    when(blackListRepository.existsByUserEmailHashed(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> userService.createUser("blocked@example.com", "password1234")
    );

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
  }

  @Test
  void createUserSucceedsWhenEmailIsNotBlacklisted() {
    User user = new User(UUID.randomUUID(), "ok@example.com", passwordEncoder.encode("password1234"));

    when(blackListRepository.existsByUserEmailHashed(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
    when(userRepository.existsByEmail("ok@example.com")).thenReturn(false);
    when(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(user);

    User created = userService.createUser("ok@example.com", "password1234");
    assertEquals("ok@example.com", created.getEmail());
  }

  @Test
  void createUserThrowsConflictWhenEmailAlreadyExists() {
    when(blackListRepository.existsByUserEmailHashed(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
    when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> userService.createUser("dup@example.com", "password1234")
    );

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
  }
}
