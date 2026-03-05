package com.dadumserver.user.presentation.dto.request;

public record UserUpdateRequest(
    String email,
    String password
) {

}
