package com.cowork.booking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.cowork.booking.common.AppConstants.Limits.EMAIL_MAX;
import static com.cowork.booking.common.AppConstants.Limits.FULL_NAME_MAX;
import static com.cowork.booking.common.AppConstants.Limits.PASSWORD_MAX;
import static com.cowork.booking.common.AppConstants.Limits.PASSWORD_MIN;
import static com.cowork.booking.common.AppConstants.Messages.Validation.EMAIL_FORMAT;
import static com.cowork.booking.common.AppConstants.Messages.Validation.LENGTH_RANGE;
import static com.cowork.booking.common.AppConstants.Messages.Validation.MAX_LENGTH;
import static com.cowork.booking.common.AppConstants.Messages.Validation.NOT_BLANK;

@Schema(description = "Datos de registro. El usuario se crea siempre con rol USER")
public record RegisterRequest(
        @Schema(example = "ana@coworking.com")
        @NotBlank(message = NOT_BLANK) @Email(message = EMAIL_FORMAT) @Size(max = EMAIL_MAX, message = MAX_LENGTH)
        String email,

        @Schema(example = "Secreta123", minLength = PASSWORD_MIN, maxLength = PASSWORD_MAX)
        @NotBlank(message = NOT_BLANK) @Size(min = PASSWORD_MIN, max = PASSWORD_MAX, message = LENGTH_RANGE)
        String password,

        @Schema(example = "Ana Pérez")
        @NotBlank(message = NOT_BLANK) @Size(max = FULL_NAME_MAX, message = MAX_LENGTH)
        String fullName
) {

    // Never log the password
    @Override
    public String toString() {
        return "RegisterRequest[email=%s, fullName=%s]".formatted(email, fullName);
    }
}
