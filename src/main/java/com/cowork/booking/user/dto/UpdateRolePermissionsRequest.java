package com.cowork.booking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

import static com.cowork.booking.common.AppConstants.Messages.Validation.NOT_EMPTY;

public record UpdateRolePermissionsRequest(
        @Schema(description = "Reemplaza todos los permisos del rol", example = "[\"SPACE_READ\", \"RESERVATION_READ_ALL\"]")
        @NotEmpty(message = NOT_EMPTY)
        Set<String> permissions
) {
}
