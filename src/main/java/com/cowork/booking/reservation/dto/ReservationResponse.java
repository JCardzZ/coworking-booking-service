package com.cowork.booking.reservation.dto;

import com.cowork.booking.common.ApiDocs.Examples;
import com.cowork.booking.reservation.model.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

public record ReservationResponse(
        @Schema(example = "10") Long id,
        @Schema(example = "1") Long spaceId,
        @Schema(example = "Sala Andes") String spaceName,
        @Schema(example = "2") Long userId,
        @Schema(example = "manuel.user@coworking.com") String userEmail,
        @Schema(example = "2026-10-01T09:00:00Z") Instant startAt,
        @Schema(example = "2026-10-01T11:00:00Z") Instant endAt,
        @Schema(example = "PENDING_PAYMENT") ReservationStatus status,
        @Schema(description = "Tarifa por hora x duración", example = "50.00") BigDecimal totalAmount,
        @Schema(example = Examples.TIMESTAMP) Instant createdAt,
        @Schema(description = "Solo si está cancelada") Instant cancelledAt
) {
}
