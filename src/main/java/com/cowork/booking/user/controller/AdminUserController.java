package com.cowork.booking.user.controller;

import com.cowork.booking.common.ApiDocs;
import com.cowork.booking.common.ApiErrorSchemas.ConflictProblem;
import com.cowork.booking.common.ApiErrorSchemas.NotFoundProblem;
import com.cowork.booking.common.ApiErrorSchemas.Problem;
import com.cowork.booking.common.ApiErrorSchemas.ValidationProblem;
import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.common.AppConstants.Paging;
import com.cowork.booking.common.AppConstants.Permissions;
import com.cowork.booking.user.dto.AdminCreateUserRequest;
import com.cowork.booking.user.dto.UpdateUserStatusRequest;
import com.cowork.booking.user.dto.UserResponse;
import com.cowork.booking.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Api.ADMIN_USERS)
@Tag(name = ApiDocs.Tags.ADMIN)
@PreAuthorize(Permissions.HAS_USER_MANAGE)
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(operationId = "adminListUsers", summary = "Listar usuarios",
            description = "Requiere permiso USER_MANAGE. Lista paginada, ordenada por email por defecto.")
    @ApiResponse(responseCode = "200", description = "Página de usuarios")
    @ApiResponse(responseCode = "400", description = "Paginación u ordenamiento inválido",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ValidationProblem.class)))
    public PagedModel<UserResponse> findAll(@ParameterObject @PageableDefault(size = Paging.DEFAULT_SIZE,
            sort = Paging.USER_DEFAULT_SORT) Pageable pageable) {
        return new PagedModel<>(adminUserService.findAll(pageable));
    }

    @GetMapping(Api.USER_ID)
    @Operation(operationId = "adminGetUserById", summary = "Obtener un usuario", description = "Requiere permiso USER_MANAGE.")
    @ApiResponse(responseCode = "200", description = "El usuario")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = NotFoundProblem.class)))
    public UserResponse findById(@Parameter(description = "Id del usuario", example = "2") @PathVariable Long userId) {
        return adminUserService.findById(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "adminCreateUser", summary = "Crear un usuario con rol",
            description = "Requiere permiso USER_MANAGE. Única vía para crear administradores u otros roles distintos de USER.")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ValidationProblem.class)))
    @ApiResponse(responseCode = "409", description = "Email ya registrado",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ConflictProblem.class)))
    @ApiResponse(responseCode = "422", description = "El rol no existe",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    public UserResponse create(@Valid @RequestBody AdminCreateUserRequest request) {
        return adminUserService.create(request);
    }

    @PutMapping(Api.USER_ID + Api.STATUS)
    @Operation(operationId = "adminUpdateUserStatus", summary = "Bloquear o activar un usuario",
            description = "Requiere permiso USER_MANAGE. Un usuario bloqueado no puede iniciar sesión y sus tokens "
                    + "dejan de ser válidos de inmediato. Un administrador no puede cambiar su propio estado.")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "400", description = "Estado inválido",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ValidationProblem.class)))
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = NotFoundProblem.class)))
    @ApiResponse(responseCode = "422", description = "Un administrador no puede cambiar su propio estado",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    public UserResponse updateStatus(@Parameter(description = "Id del usuario", example = "2") @PathVariable Long userId,
                                     @Valid @RequestBody UpdateUserStatusRequest request) {
        return adminUserService.updateStatus(userId, request);
    }
}
