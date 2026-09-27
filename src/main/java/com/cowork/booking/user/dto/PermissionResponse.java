package com.cowork.booking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PermissionResponse(
        @Schema(example = "SPACE_WRITE") String code,
        @Schema(example = "Crear, modificar y desactivar espacios") String description
) {
}
