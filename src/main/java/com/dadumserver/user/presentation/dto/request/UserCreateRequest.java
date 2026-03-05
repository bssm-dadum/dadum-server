package com.dadumserver.user.presentation.dto.request;

public record UserCreateRequest(
    String email,
    String password
) {
}
