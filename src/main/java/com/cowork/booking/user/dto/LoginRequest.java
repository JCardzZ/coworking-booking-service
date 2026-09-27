package com.cowork.booking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import static com.cowork.booking.common.AppConstants.Messages.Validation.EMAIL_FORMAT;
import static com.cowork.booking.common.AppConstants.Messages.Validation.NOT_BLANK;

public record LoginRequest(
        @Schema(example = "ana@coworking.com")
        @NotBlank(message = NOT_BLANK) @Email(message = EMAIL_FORMAT)
        String email,

        @Schema(example = "Secreta123")
        @NotBlank(message = NOT_BLANK)
        String password
) {

    // Never log the password
    @Override
    public String toString() {
        return "LoginRequest[email=%s]".formatted(email);
    }
}
