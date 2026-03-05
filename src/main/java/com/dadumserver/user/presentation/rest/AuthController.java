package com.dadumserver.user.presentation.rest;

import com.dadumserver.user.application.dto.LoginCommand;
import com.dadumserver.user.application.dto.LoginResult;
import com.dadumserver.user.application.dto.RefreshCommand;
import com.dadumserver.user.application.service.UserService;
import com.dadumserver.user.presentation.dto.request.LoginRequest;
import com.dadumserver.user.presentation.dto.request.RefreshTokenRequest;
import com.dadumserver.user.presentation.dto.response.LoginResponse;
import lombok.RequiredArgsConstructor;
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

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    LoginResult result = userService.login(new LoginCommand(request.email(), request.password()));
    return ResponseEntity.ok(toResponse(result));
  }

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
