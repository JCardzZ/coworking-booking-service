package com.cowork.booking.space.dto;

import com.cowork.booking.space.model.SpaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

import static com.cowork.booking.common.AppConstants.Limits.MONEY_FRACTION_DIGITS;
import static com.cowork.booking.common.AppConstants.Limits.MONEY_INTEGER_DIGITS;
import static com.cowork.booking.common.AppConstants.Limits.SPACE_LOCATION_MAX;
import static com.cowork.booking.common.AppConstants.Limits.SPACE_NAME_MAX;
import static com.cowork.booking.common.AppConstants.Messages.Validation.DECIMAL_FORMAT;
import static com.cowork.booking.common.AppConstants.Messages.Validation.MAX_LENGTH;
import static com.cowork.booking.common.AppConstants.Messages.Validation.NOT_BLANK;
import static com.cowork.booking.common.AppConstants.Messages.Validation.POSITIVE;
import static com.cowork.booking.common.AppConstants.Messages.Validation.REQUIRED;

@Schema(name = "SpaceRequest", description = "Datos para crear o reemplazar un espacio")
public record SpaceRequest(
        @Schema(description = "Nombre único entre los espacios activos (sin distinguir mayúsculas)", example = "Sala Andes")
        @NotBlank(message = NOT_BLANK) @Size(max = SPACE_NAME_MAX, message = MAX_LENGTH)
        String name,

        @Schema(description = "Tipo de espacio", example = "MEETING_ROOM")
        @NotNull(message = REQUIRED)
        SpaceType type,

        @Schema(description = "Número máximo de personas", example = "8", minimum = "1")
        @NotNull(message = REQUIRED) @Positive(message = POSITIVE)
        Integer capacity,

        @Schema(description = "Ubicación del espacio", example = "Piso 2 - Ala norte")
        @NotBlank(message = NOT_BLANK) @Size(max = SPACE_LOCATION_MAX, message = MAX_LENGTH)
        String location,

        @Schema(description = "Tarifa por hora", example = "25.00", minimum = "0.01")
        @NotNull(message = REQUIRED) @Positive(message = POSITIVE)
        @Digits(integer = MONEY_INTEGER_DIGITS, fraction = MONEY_FRACTION_DIGITS, message = DECIMAL_FORMAT)
        BigDecimal hourlyRate
) {
}
