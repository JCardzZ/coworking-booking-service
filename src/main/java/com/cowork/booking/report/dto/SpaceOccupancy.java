package com.cowork.booking.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record SpaceOccupancy(
        @Schema(example = "1") Long spaceId,
        @Schema(example = "Sala Andes") String spaceName,
        @Schema(description = "Reservas confirmadas que caen en el rango", example = "12") long confirmedReservations,
        @Schema(description = "Horas reservadas dentro del rango", example = "36.00") BigDecimal reservedHours,
        @Schema(description = "Horas del rango (24 por día)", example = "720") BigDecimal availableHours,
        @Schema(description = "reservedHours / availableHours en %", example = "5.00") BigDecimal occupancyPercent
) {
}
