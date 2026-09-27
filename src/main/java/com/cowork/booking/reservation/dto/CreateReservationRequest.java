package com.cowork.booking.reservation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

import static com.cowork.booking.common.AppConstants.Messages.Validation.END_AFTER_START;
import static com.cowork.booking.common.AppConstants.Messages.Validation.POSITIVE;
import static com.cowork.booking.common.AppConstants.Messages.Validation.REQUIRED;

public record CreateReservationRequest(
        @Schema(description = "Id del espacio", example = "1")
        @NotNull(message = REQUIRED) @Positive(message = POSITIVE)
        Long spaceId,

        @Schema(description = "Inicio (ISO-8601 con zona)", example = "2027-01-15T09:00:00Z")
        @NotNull(message = REQUIRED)
        Instant startAt,

        @Schema(description = "Fin, exclusivo: otra reserva puede empezar justo a esta hora", example = "2027-01-15T11:00:00Z")
        @NotNull(message = REQUIRED)
        Instant endAt
) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = END_AFTER_START)
    public boolean isValidPeriod() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }
}
