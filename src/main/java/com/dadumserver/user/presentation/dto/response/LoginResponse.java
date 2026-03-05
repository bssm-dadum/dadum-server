package com.dadumserver.user.presentation.dto.response;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long accessTokenExpiresIn,
    String refreshToken,
    long refreshTokenExpiresIn
) {
}
