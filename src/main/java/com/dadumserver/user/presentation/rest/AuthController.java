package com.dadumserver.user.presentation.rest;

import com.dadumserver.user.application.dto.LoginCommand;
import com.dadumserver.user.application.dto.LoginResult;
import com.dadumserver.user.application.dto.RefreshCommand;
import com.dadumserver.user.application.service.UserService;
import com.dadumserver.user.domain.model.User;
import com.dadumserver.user.presentation.dto.request.LoginRequest;
import com.dadumserver.user.presentation.dto.request.RefreshTokenRequest;
import com.dadumserver.user.presentation.dto.request.UserCreateRequest;
import com.dadumserver.user.presentation.dto.response.LoginResponse;
import com.dadumserver.user.presentation.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
  private final UserService userService;

  // 로그인
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    LoginResult result = userService.login(new LoginCommand(request.email(), request.password()));
    return ResponseEntity.ok(toResponse(result));
  }

  // 회원 가입
  @PostMapping("/signup")
  public ResponseEntity<UserResponse> createUser(@RequestBody UserCreateRequest request) {
    User user = userService.createUser(request.email(), request.password());
    return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
  }

  // jwt token 갱신
  @PostMapping("/refresh")
  public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshTokenRequest request) {
    LoginResult result = userService.refresh(new RefreshCommand(request.refreshToken()));
    return ResponseEntity.ok(toResponse(result));
  }

  private LoginResponse toResponse(LoginResult result) {
    return new LoginResponse(
        result.accessToken(),
        "Bearer",
        result.accessTokenExpiresIn(),
        result.refreshToken(),
        result.refreshTokenExpiresIn()
    );
  }
}
