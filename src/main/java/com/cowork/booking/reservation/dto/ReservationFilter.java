package com.cowork.booking.reservation.dto;

import com.cowork.booking.reservation.model.ReservationStatus;
import io.swagger.v3.oas.annotations.Parameter;

import java.time.Instant;

/** Optional query parameters for listing reservations. */
public record ReservationFilter(
        @Parameter(description = "Id del espacio", example = "1") Long spaceId,
        @Parameter(description = "Estado", example = "CONFIRMED") ReservationStatus status,
        @Parameter(description = "Reservas que terminan después de esta fecha", example = "2026-10-01T00:00:00Z") Instant from,
        @Parameter(description = "Reservas que empiezan antes de esta fecha", example = "2026-10-31T00:00:00Z") Instant to,
        @Parameter(description = "Solo con RESERVATION_READ_ALL: filtra por usuario", example = "2") Long userId
) {
}
