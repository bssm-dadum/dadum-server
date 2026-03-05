package com.dadumserver.user.presentation.rest;

import com.dadumserver.user.presentation.dto.response.MeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
  @GetMapping("/me")
  public ResponseEntity<MeResponse> me(Authentication authentication) {
    UUID userId = (UUID) authentication.getPrincipal();
    return ResponseEntity.ok(new MeResponse(userId));
  }
}
