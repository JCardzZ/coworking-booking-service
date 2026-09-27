package com.cowork.booking.payment.dto;

import com.cowork.booking.common.ApiDocs.Examples;
import com.cowork.booking.payment.model.DeclineReason;
import com.cowork.booking.payment.model.PaymentOutcome;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentAttemptResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "CARD") String methodType,
        @Schema(description = "Solo los últimos 4 caracteres", example = "****4242") String maskedInstrument,
        @Schema(example = "20.00") BigDecimal amount,
        @Schema(example = "DECLINED") PaymentOutcome outcome,
        @Schema(description = "Solo si fue rechazado", example = "INSUFFICIENT_FUNDS") DeclineReason declineReason,
        @Schema(description = "Solo si fue aprobado", example = "pay_8f3k2m9x1q7w") String providerReference,
        @Schema(description = "Cuánto tardó el proveedor, reintentos incluidos", example = "184") long durationMs,
        @Schema(example = Examples.TIMESTAMP) Instant createdAt
) {
}
