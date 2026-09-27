package com.cowork.booking.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "payment.service")
public record PaymentServiceProperties(@NotBlank String url, @NotNull Duration connectTimeout,
                                       @NotNull Duration readTimeout) {
}
