package com.cowork.booking.user.controller;

import com.cowork.booking.common.ApiDocs;
import com.cowork.booking.common.ApiErrorSchemas.ConflictProblem;
import com.cowork.booking.common.ApiErrorSchemas.NotFoundProblem;
import com.cowork.booking.common.ApiErrorSchemas.Problem;
import com.cowork.booking.common.ApiErrorSchemas.ValidationProblem;
import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.common.AppConstants.Permissions;
import com.cowork.booking.user.dto.CreateRoleRequest;
import com.cowork.booking.user.dto.PermissionResponse;
import com.cowork.booking.user.dto.RoleResponse;
import com.cowork.booking.user.dto.UpdateRolePermissionsRequest;
import com.cowork.booking.user.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = ApiDocs.Tags.ADMIN)
@PreAuthorize(Permissions.HAS_RBAC_MANAGE)
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleService roleService;

    @GetMapping(Api.ADMIN_PERMISSIONS)
    @Operation(operationId = "adminListPermissions", summary = "Catálogo de permisos",
            description = "Requiere permiso RBAC_MANAGE. Permisos que se pueden asignar a los roles.")
    @ApiResponse(responseCode = "200", description = "Permisos disponibles")
    public List<PermissionResponse> findAllPermissions() {
        return roleService.findAllPermissions();
    }

    @GetMapping(Api.ADMIN_ROLES)
    @Operation(operationId = "adminListRoles", summary = "Listar roles", description = "Requiere permiso RBAC_MANAGE.")
    @ApiResponse(responseCode = "200", description = "Roles con sus permisos")
    public List<RoleResponse> findAll() {
        return roleService.findAll();
    }

    @PostMapping(Api.ADMIN_ROLES)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "adminCreateRole", summary = "Crear un rol",
            description = "Requiere permiso RBAC_MANAGE. Los permisos deben existir en el catálogo.")
    @ApiResponse(responseCode = "201", description = "Rol creado")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ValidationProblem.class)))
    @ApiResponse(responseCode = "409", description = "Nombre de rol en uso",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ConflictProblem.class)))
    @ApiResponse(responseCode = "422", description = "Algún permiso no existe",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    public RoleResponse create(@Valid @RequestBody CreateRoleRequest request) {
        return roleService.create(request);
    }

    @PutMapping(Api.ADMIN_ROLES + Api.ROLE_ID + Api.PERMISSIONS)
    @Operation(operationId = "adminUpdateRolePermissions", summary = "Reemplazar los permisos de un rol",
            description = "Requiere permiso RBAC_MANAGE. Se aplica de inmediato a todos los usuarios del rol. "
                    + "El rol ADMIN no puede perder USER_MANAGE ni RBAC_MANAGE.")
    @ApiResponse(responseCode = "200", description = "Permisos actualizados")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ValidationProblem.class)))
    @ApiResponse(responseCode = "404", description = "Rol no encontrado",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = NotFoundProblem.class)))
    @ApiResponse(responseCode = "422", description = "Permiso inexistente o el rol ADMIN perdería la administración",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    public RoleResponse updatePermissions(@Parameter(description = "Id del rol", example = "3") @PathVariable Long roleId,
                                          @Valid @RequestBody UpdateRolePermissionsRequest request) {
        return roleService.updatePermissions(roleId, request);
    }
}
