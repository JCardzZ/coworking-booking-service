package com.cowork.booking.user.dto;

import com.cowork.booking.user.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import static com.cowork.booking.common.AppConstants.Messages.Validation.REQUIRED;

public record UpdateUserStatusRequest(
        @Schema(description = "DISABLED bloquea el login y los tokens ya emitidos", example = "DISABLED")
        @NotNull(message = REQUIRED)
        UserStatus status
) {
}
