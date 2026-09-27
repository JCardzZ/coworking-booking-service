package com.cowork.booking.user.dto;

import com.cowork.booking.common.ApiDocs.Examples;
import com.cowork.booking.user.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record UserResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "ana@coworking.com") String email,
        @Schema(example = "Ana Pérez") String fullName,
        @Schema(description = "Nombre del rol", example = "USER") String role,
        @Schema(example = "ACTIVE") UserStatus status,
        @Schema(example = Examples.TIMESTAMP) Instant createdAt
) {
}
