package com.cowork.booking.user.controller;

import com.cowork.booking.common.ApiDocs;
import com.cowork.booking.common.ApiErrorSchemas.ConflictProblem;
import com.cowork.booking.common.ApiErrorSchemas.Problem;
import com.cowork.booking.common.ApiErrorSchemas.ValidationProblem;
import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.user.dto.LoginRequest;
import com.cowork.booking.user.dto.RegisterRequest;
import com.cowork.booking.user.dto.TokenResponse;
import com.cowork.booking.user.dto.UserResponse;
import com.cowork.booking.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Api.AUTH)
@Tag(name = ApiDocs.Tags.AUTH)
@SecurityRequirements
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping(Api.REGISTER)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "register", summary = "Registrar un usuario",
            description = "Crea una cuenta con rol USER. Los administradores no se pueden registrar por esta vía.")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ValidationProblem.class)))
    @ApiResponse(responseCode = "409", description = "Email ya registrado",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ConflictProblem.class)))
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping(Api.LOGIN)
    @Operation(operationId = "login", summary = "Iniciar sesión",
            description = "Devuelve un JWT con el rol del usuario. Úsalo en la cabecera Authorization: Bearer <token>.")
    @ApiResponse(responseCode = "200", description = "Token emitido")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ValidationProblem.class)))
    @ApiResponse(responseCode = "401", description = "Email o contraseña incorrectos",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
