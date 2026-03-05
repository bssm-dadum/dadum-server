package com.dadumserver.user.application.dto;

public record LoginCommand(
    String email,
    String password
) {
}
