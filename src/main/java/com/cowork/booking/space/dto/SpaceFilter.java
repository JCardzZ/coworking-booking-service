package com.cowork.booking.space.dto;

import com.cowork.booking.space.model.SpaceType;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Positive;

import static com.cowork.booking.common.AppConstants.Messages.Validation.POSITIVE;

/** Optional query parameters for listing spaces. */
public record SpaceFilter(
        @Parameter(description = "Tipo de espacio exacto", example = "MEETING_ROOM")
        SpaceType type,

        @Parameter(description = "Capacidad mínima", example = "4")
        @Positive(message = POSITIVE)
        Integer minCapacity,

        @Parameter(description = "Coincidencia parcial en la ubicación, sin distinguir mayúsculas", example = "piso 2")
        String location
) {
}
