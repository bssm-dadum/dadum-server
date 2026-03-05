package com.dadumserver.user.presentation.dto.request;

import java.util.UUID;

public record UserBlacklistRequest(
    UUID userId
) {

}
