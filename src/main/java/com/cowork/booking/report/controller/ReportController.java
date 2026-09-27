package com.cowork.booking.report.controller;

import com.cowork.booking.common.ApiDocs;
import com.cowork.booking.common.ApiErrorSchemas.NotFoundProblem;
import com.cowork.booking.common.ApiErrorSchemas.ValidationProblem;
import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.common.AppConstants.Permissions;
import com.cowork.booking.report.dto.OccupancyReportRequest;
import com.cowork.booking.report.dto.OccupancyReportResponse;
import com.cowork.booking.report.service.OccupancyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Api.REPORTS)
@Tag(name = ApiDocs.Tags.REPORTS)
@RequiredArgsConstructor
public class ReportController {

    private final OccupancyReportService occupancyReportService;

    @GetMapping(Api.OCCUPANCY)
    @PreAuthorize(Permissions.HAS_REPORT_READ)
    @Operation(operationId = "getOccupancyReport", summary = "Reporte de ocupación por espacio",
            description = "Requiere permiso REPORT_READ. Porcentaje de horas con reservas CONFIRMED sobre las horas del "
                    + "rango (24 por día, UTC, ambos días incluidos, máximo 366 días). Las reservas que cruzan los bordes "
                    + "solo cuentan la parte dentro del rango. El resultado se cachea y se invalida al confirmar o cancelar "
                    + "una reserva y al modificar un espacio.")
    @ApiResponse(responseCode = "200", description = "Ocupación de cada espacio activo, ordenada por nombre")
    @ApiResponse(responseCode = "400", description = "Fechas ausentes o rango inválido",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ValidationProblem.class)))
    @ApiResponse(responseCode = "404", description = "Espacio no encontrado",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = NotFoundProblem.class)))
    public OccupancyReportResponse occupancy(@ParameterObject @Valid OccupancyReportRequest request) {
        return occupancyReportService.occupancy(request);
    }
}
