package com.dadumserver.user.presentation.dto.response;

import com.dadumserver.user.domain.model.User;

import java.util.UUID;

public record UserResponse(
    UUID id,
    String email
) {
  public static UserResponse from(User user) {
    return new UserResponse(user.getId(), user.getEmail());
  }
}
