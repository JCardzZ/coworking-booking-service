package com.cowork.booking.space.dto;

import com.cowork.booking.common.ApiDocs.Examples;
import com.cowork.booking.space.model.SpaceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(name = "SpaceResponse", description = "Espacio de coworking reservable")
public record SpaceResponse(
        @Schema(description = "Id del espacio", example = "1") Long id,
        @Schema(description = "Nombre único entre los espacios activos (sin distinguir mayúsculas)", example = "Sala Andes") String name,
        @Schema(description = "Tipo de espacio", example = "MEETING_ROOM") SpaceType type,
        @Schema(description = "Número máximo de personas", example = "8") int capacity,
        @Schema(description = "Ubicación del espacio", example = "Piso 2 - Ala norte") String location,
        @Schema(description = "Tarifa por hora", example = "25.00") BigDecimal hourlyRate,
        @Schema(description = "Fecha de creación (UTC)", example = Examples.TIMESTAMP) Instant createdAt,
        @Schema(description = "Fecha de última modificación (UTC)", example = Examples.TIMESTAMP) Instant updatedAt
) {
}
