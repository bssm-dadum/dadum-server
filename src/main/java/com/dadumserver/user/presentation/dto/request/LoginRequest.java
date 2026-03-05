package com.dadumserver.user.presentation.dto.request;

public record LoginRequest(
    String email,
    String password
) {
}
