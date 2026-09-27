package com.cowork.booking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

import static com.cowork.booking.common.AppConstants.Limits.DESCRIPTION_MAX;
import static com.cowork.booking.common.AppConstants.Limits.ROLE_NAME_PATTERN;
import static com.cowork.booking.common.AppConstants.Messages.Validation.MAX_LENGTH;
import static com.cowork.booking.common.AppConstants.Messages.Validation.NOT_BLANK;
import static com.cowork.booking.common.AppConstants.Messages.Validation.NOT_EMPTY;
import static com.cowork.booking.common.AppConstants.Messages.Validation.ROLE_NAME_FORMAT;

public record CreateRoleRequest(
        @Schema(example = "RECEPTION")
        @NotBlank(message = NOT_BLANK) @Pattern(regexp = ROLE_NAME_PATTERN, message = ROLE_NAME_FORMAT)
        String name,

        @Schema(example = "Recepción: consulta espacios y reservas")
        @NotBlank(message = NOT_BLANK) @Size(max = DESCRIPTION_MAX, message = MAX_LENGTH)
        String description,

        @Schema(description = "Códigos del catálogo GET /admin/permissions", example = "[\"SPACE_READ\", \"RESERVATION_READ_ALL\"]")
        @NotEmpty(message = NOT_EMPTY)
        Set<String> permissions
) {
}
