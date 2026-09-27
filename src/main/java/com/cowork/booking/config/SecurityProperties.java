package com.cowork.booking.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(@Valid @NotNull Jwt jwt, @Valid @NotNull Admin admin) {

    /** HS256 needs a key of at least 256 bits (32 bytes). */
    public record Jwt(@NotBlank @Size(min = 32) String secret, @NotNull Duration expiration) {
    }

    /** Initial administrator, created on startup if missing. */
    public record Admin(@NotBlank @Email String email, @NotBlank @Size(min = 8) String password, @NotBlank String fullName) {
    }
}
