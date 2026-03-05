package com.dadumserver.user.application.dto;

public record LoginResult(
    String accessToken,
    long accessTokenExpiresIn,
    String refreshToken,
    long refreshTokenExpiresIn
) {
}
