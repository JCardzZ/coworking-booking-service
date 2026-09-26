package com.cowork.booking.common;

import com.cowork.booking.common.ApiDocs.Examples;
import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Messages.Common;
import com.cowork.booking.common.AppConstants.Messages.Validation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/** OpenAPI-only schemas of the error bodies built by {@link GlobalExceptionHandler}. */
public final class ApiErrorSchemas {

    public static final String PROBLEM_SCHEMA = "Problem";

    private static final String TYPE = "URI que identifica el tipo de problema";
    private static final String TITLE = "Resumen legible del tipo de problema";
    private static final String STATUS = "Código de estado HTTP";
    private static final String DETAIL = "Explicación específica de esta ocurrencia";
    private static final String INSTANCE = "Ruta de la solicitud que produjo el problema";
    private static final String CODE = "Código de error estable, legible por máquina";
    private static final String TIMESTAMP = "Momento en que ocurrió el error (UTC)";
    private static final String TRACE_ID = "Id de correlación (cabecera X-Correlation-Id); úsalo para buscar la solicitud en los logs";

    private ApiErrorSchemas() {
    }

    @Schema(name = PROBLEM_SCHEMA, description = "Problem Details RFC 9457 (error genérico)")
    public record Problem(
            @Schema(description = TYPE, example = Examples.PROBLEM_TYPE_BASE + "internal-error") String type,
            @Schema(description = TITLE, example = Common.INTERNAL_ERROR_TITLE) String title,
            @Schema(description = STATUS, example = "500") int status,
            @Schema(description = DETAIL, example = Common.INTERNAL_ERROR_DETAIL) String detail,
            @Schema(description = INSTANCE, example = Examples.API_PATH + "/spaces") String instance,
            @Schema(description = CODE, example = ErrorCodes.INTERNAL_ERROR) String code,
            @Schema(description = TIMESTAMP, example = Examples.TIMESTAMP) Instant timestamp,
            @Schema(description = TRACE_ID, example = Examples.TRACE_ID) String traceId) {
    }

    @Schema(name = "ValidationProblem", description = "Problem Details RFC 9457 para datos de entrada inválidos (400)")
    public record ValidationProblem(
            @Schema(description = TYPE, example = Examples.PROBLEM_TYPE_BASE + "validation-error") String type,
            @Schema(description = TITLE, example = Common.VALIDATION_FAILED_TITLE) String title,
            @Schema(description = STATUS, example = "400") int status,
            @Schema(description = DETAIL, example = Common.VALIDATION_FAILED_DETAIL) String detail,
            @Schema(description = INSTANCE, example = Examples.API_PATH + "/spaces") String instance,
            @Schema(description = CODE, example = ErrorCodes.VALIDATION_ERROR) String code,
            @Schema(description = TIMESTAMP, example = Examples.TIMESTAMP) Instant timestamp,
            @Schema(description = TRACE_ID, example = Examples.TRACE_ID) String traceId,
            @Schema(description = "Un elemento por cada campo inválido") List<FieldViolation> errors) {
    }

    @Schema(name = "FieldViolation")
    public record FieldViolation(
            @Schema(description = "Campo inválido", example = "capacity") String field,
            @Schema(description = "Restricción incumplida", example = "POSITIVE") String code,
            @Schema(description = "Mensaje legible", example = Validation.POSITIVE) String message) {
    }

    @Schema(name = "NotFoundProblem", description = "Problem Details RFC 9457 para un recurso inexistente (404)")
    public record NotFoundProblem(
            @Schema(description = TYPE, example = Examples.PROBLEM_TYPE_BASE + "resource-not-found") String type,
            @Schema(description = TITLE, example = Common.NOT_FOUND_TITLE) String title,
            @Schema(description = STATUS, example = "404") int status,
            @Schema(description = DETAIL, example = "Espacio con id 42 no encontrado") String detail,
            @Schema(description = INSTANCE, example = Examples.API_PATH + "/spaces/42") String instance,
            @Schema(description = CODE, example = ErrorCodes.RESOURCE_NOT_FOUND) String code,
            @Schema(description = TIMESTAMP, example = Examples.TIMESTAMP) Instant timestamp,
            @Schema(description = TRACE_ID, example = Examples.TRACE_ID) String traceId,
            @Schema(description = "Tipo del recurso no encontrado", example = "Space") String resourceType,
            @Schema(description = "Id del recurso no encontrado", example = "42") String resourceId) {
    }

    @Schema(name = "ConflictProblem", description = "Problem Details RFC 9457 para un conflicto de negocio o de estado (409)")
    public record ConflictProblem(
            @Schema(description = TYPE, example = Examples.PROBLEM_TYPE_BASE + "space-name-taken") String type,
            @Schema(description = TITLE, example = Common.BUSINESS_RULE_TITLE) String title,
            @Schema(description = STATUS, example = "409") int status,
            @Schema(description = DETAIL, example = "Ya existe un espacio con el nombre 'Sala Andes'") String detail,
            @Schema(description = INSTANCE, example = Examples.API_PATH + "/spaces") String instance,
            @Schema(description = CODE, example = ErrorCodes.SPACE_NAME_TAKEN) String code,
            @Schema(description = TIMESTAMP, example = Examples.TIMESTAMP) Instant timestamp,
            @Schema(description = TRACE_ID, example = Examples.TRACE_ID) String traceId,
            @Schema(description = "Campo que causa el conflicto, cuando aplica", example = "name") String conflictingField) {
    }
}
