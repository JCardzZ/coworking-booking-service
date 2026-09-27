package com.cowork.booking.reservation.controller;

import com.cowork.booking.common.ApiDocs;
import com.cowork.booking.common.ApiErrorSchemas.ConflictProblem;
import com.cowork.booking.common.ApiErrorSchemas.NotFoundProblem;
import com.cowork.booking.common.ApiErrorSchemas.Problem;
import com.cowork.booking.common.ApiErrorSchemas.ValidationProblem;
import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.common.AppConstants.Limits;
import com.cowork.booking.common.AppConstants.Messages.Validation;
import com.cowork.booking.common.AppConstants.Paging;
import com.cowork.booking.common.AppConstants.Permissions;
import com.cowork.booking.reservation.dto.CreateReservationRequest;
import com.cowork.booking.reservation.dto.ReservationFilter;
import com.cowork.booking.reservation.dto.ReservationResponse;
import com.cowork.booking.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping(Api.RESERVATIONS)
@Tag(name = ApiDocs.Tags.RESERVATIONS)
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize(Permissions.HAS_RESERVATION_CREATE)
    @Operation(operationId = "createReservation", summary = "Crear una reserva",
            description = "Requiere permiso RESERVATION_CREATE. Queda en PENDING_PAYMENT. No se permiten reservas solapadas "
                    + "en el mismo espacio (el fin es exclusivo). Reenviar la misma Idempotency-Key devuelve la reserva "
                    + "original (200) sin crear otra.")
    @ApiResponse(responseCode = "201", description = "Reserva creada",
            headers = @Header(name = HttpHeaders.LOCATION, description = "URL de la reserva", schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "200", description = "Misma Idempotency-Key: se devuelve la reserva ya creada")
    @ApiResponse(responseCode = "400", description = "Datos o cabecera Idempotency-Key inválidos",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ValidationProblem.class)))
    @ApiResponse(responseCode = "404", description = "Espacio no encontrado",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = NotFoundProblem.class)))
    @ApiResponse(responseCode = "409", description = "El espacio ya está reservado en ese horario",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ConflictProblem.class)))
    @ApiResponse(responseCode = "422", description = "Fecha pasada, duración mayor a 12 h o Idempotency-Key reutilizada con otros datos",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    public ResponseEntity<ReservationResponse> create(
            @Parameter(description = "Clave única por intento de reserva (p. ej. un UUID)", required = true,
                    example = "3f1c9a52-8d4e-4b7a-9c61-2e5f0b7d4a18")
            @RequestHeader(Api.IDEMPOTENCY_KEY_HEADER)
            @Pattern(regexp = Limits.IDEMPOTENCY_KEY_PATTERN, message = Validation.IDEMPOTENCY_KEY_FORMAT) String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest request) {
        ReservationService.CreateResult result = reservationService.create(request, idempotencyKey);
        if (!result.created()) {
            return ResponseEntity.ok(result.reservation());
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path(Api.RESERVATION_ID).buildAndExpand(result.reservation().id()).toUri();
        return ResponseEntity.created(location).body(result.reservation());
    }

    @GetMapping
    @PreAuthorize(Permissions.CAN_READ_RESERVATIONS)
    @Operation(operationId = "listReservations", summary = "Listar reservas",
            description = "Con RESERVATION_READ_OWN devuelve solo las propias; con RESERVATION_READ_ALL, todas. "
                    + "Por defecto de la más reciente a la más antigua.")
    @ApiResponse(responseCode = "200", description = "Página de reservas")
    @ApiResponse(responseCode = "400", description = "Filtro, paginación u ordenamiento inválido",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ValidationProblem.class)))
    public PagedModel<ReservationResponse> findAll(@ParameterObject ReservationFilter filter,
                                                   @ParameterObject @PageableDefault(size = Paging.DEFAULT_SIZE,
                                                           sort = Paging.RESERVATION_DEFAULT_SORT,
                                                           direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(reservationService.findAll(filter, pageable));
    }

    @GetMapping(Api.RESERVATION_ID)
    @PreAuthorize(Permissions.CAN_READ_RESERVATIONS)
    @Operation(operationId = "getReservationById", summary = "Obtener una reserva",
            description = "Las reservas de otros usuarios se informan como no encontradas, salvo con RESERVATION_READ_ALL.")
    @ApiResponse(responseCode = "200", description = "La reserva")
    @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = NotFoundProblem.class)))
    public ReservationResponse findById(@Parameter(description = "Id de la reserva", example = "10") @PathVariable Long reservationId) {
        return reservationService.findById(reservationId);
    }

    @PostMapping(Api.RESERVATION_ID + Api.CONFIRM)
    @PreAuthorize(Permissions.CAN_CONFIRM_RESERVATIONS)
    @Operation(operationId = "confirmReservation", summary = "Confirmar y pagar una reserva",
            description = "Valida el pago contra el servicio externo, protegido con circuit breaker. Si el pago se aprueba "
                    + "pasa a CONFIRMED (200). Si el servicio de pagos no responde o el circuito está abierto, la reserva "
                    + "sigue en PENDING_PAYMENT (202) y se puede reintentar más tarde.")
    @ApiResponse(responseCode = "200", description = "Pago aprobado, reserva confirmada")
    @ApiResponse(responseCode = "202", description = "Servicio de pagos no disponible: sigue en PENDING_PAYMENT")
    @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = NotFoundProblem.class)))
    @ApiResponse(responseCode = "409", description = "La reserva no está pendiente de pago",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ConflictProblem.class)))
    @ApiResponse(responseCode = "422", description = "Pago rechazado: sigue en PENDING_PAYMENT",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    public ResponseEntity<ReservationResponse> confirm(@Parameter(description = "Id de la reserva", example = "10")
                                                       @PathVariable Long reservationId) {
        ReservationService.ConfirmResult result = reservationService.confirm(reservationId);
        return result.confirmed()
                ? ResponseEntity.ok(result.reservation())
                : ResponseEntity.accepted().body(result.reservation());
    }

    @PostMapping(Api.RESERVATION_ID + Api.CANCEL)
    @PreAuthorize(Permissions.CAN_CANCEL_RESERVATIONS)
    @Operation(operationId = "cancelReservation", summary = "Cancelar una reserva",
            description = "El dueño cancela las suyas; con RESERVATION_MANAGE_ALL, cualquiera. Solo antes de que empiece. "
                    + "Libera el horario para otras reservas.")
    @ApiResponse(responseCode = "200", description = "Reserva cancelada")
    @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = NotFoundProblem.class)))
    @ApiResponse(responseCode = "409", description = "Ya estaba cancelada o ya comenzó",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ConflictProblem.class)))
    public ReservationResponse cancel(@Parameter(description = "Id de la reserva", example = "10") @PathVariable Long reservationId) {
        return reservationService.cancel(reservationId);
    }
}
