package com.cowork.booking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
        @Schema(description = "JWT para la cabecera Authorization: Bearer <token>") String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Segundos hasta que el token expira", example = "3600") long expiresIn
) {
}
