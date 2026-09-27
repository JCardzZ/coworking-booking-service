package com.cowork.booking.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.cowork.booking.common.AppConstants.Limits.REPORT_MAX_DAYS;
import static com.cowork.booking.common.AppConstants.Messages.Validation.POSITIVE;
import static com.cowork.booking.common.AppConstants.Messages.Validation.REPORT_RANGE;
import static com.cowork.booking.common.AppConstants.Messages.Validation.REQUIRED;

/** Days in UTC, both included. Also the cache key of the report. */
public record OccupancyReportRequest(
        @Parameter(description = "Primer día del rango (UTC)", required = true, example = "2027-04-01")
        @NotNull(message = REQUIRED)
        LocalDate from,

        @Parameter(description = "Último día del rango, incluido (UTC)", required = true, example = "2027-04-30")
        @NotNull(message = REQUIRED)
        LocalDate to,

        @Parameter(description = "Solo este espacio; si se omite, todos los espacios activos", example = "1")
        @Positive(message = POSITIVE)
        Long spaceId
) {

    @JsonIgnore
    @Parameter(hidden = true)
    @AssertTrue(message = REPORT_RANGE)
    public boolean isValidRange() {
        return from == null || to == null
                || (!to.isBefore(from) && ChronoUnit.DAYS.between(from, to) < REPORT_MAX_DAYS);
    }

    public long days() {
        return ChronoUnit.DAYS.between(from, to) + 1;
    }
}
