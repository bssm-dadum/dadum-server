package com.dadumserver.user.presentation.rest;

import com.dadumserver.user.application.service.UserService;
import com.dadumserver.user.domain.model.User;
import com.dadumserver.user.presentation.dto.request.UserBlacklistRequest;
import com.dadumserver.user.presentation.dto.request.UserCreateRequest;
import com.dadumserver.user.presentation.dto.request.UserUpdateRequest;
import com.dadumserver.user.presentation.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
  private final UserService userService;

  // 사용자 단일 조회
  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> getUser(
      @PathVariable UUID id,
      Authentication authentication
  ) {
    User user = userService.getUser(id, getUserId(authentication));
    return ResponseEntity.ok(UserResponse.from(user));
  }

  // 사용자 전체 조회
  @GetMapping
  public ResponseEntity<List<UserResponse>> getUsers() {
    List<UserResponse> users = userService.getUsers().stream()
        .map(UserResponse::from)
        .toList();
    return ResponseEntity.ok(users);
  }

  // 사용자 정보 수정
  @PutMapping("/{id}")
  public ResponseEntity<UserResponse> updateUser(
      @PathVariable UUID id,
      @RequestBody UserUpdateRequest request,
      Authentication authentication
  ) {
    User user = userService.updateUser(
        id,
        getUserId(authentication),
        request.email(),
        request.password()
    );
    return ResponseEntity.ok(UserResponse.from(user));
  }

  // 사용자 삭제
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(
      @PathVariable UUID id,
      Authentication authentication
  ) {
    userService.deleteUser(id, getUserId(authentication));
    return ResponseEntity.noContent().build();
  }

  // 블랙리스트 기반 사용자 데이터 저장
  @PostMapping("/blacklist")
  public ResponseEntity<Void> blacklistUser(
      @RequestBody UserBlacklistRequest request,
      Authentication authentication
  ) {
    userService.blacklistUser(request.userId(), getUserId(authentication));
    return ResponseEntity.noContent().build();
  }

  // 사용자 데이터 추가
  @PostMapping
  public ResponseEntity<UserResponse> createUser(@RequestBody UserCreateRequest request) {
    User user = userService.createUser(request.email(), request.password());
    return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
  }

  private UUID getUserId(Authentication authentication) {
    Object principal = authentication.getPrincipal();
    if (principal instanceof UUID userId) {
      return userId;
    }
    throw new IllegalStateException("Invalid authentication principal");
  }
}
