package com.cowork.booking.space.controller;

import com.cowork.booking.common.ApiDocs;
import com.cowork.booking.common.ApiErrorSchemas.ConflictProblem;
import com.cowork.booking.common.ApiErrorSchemas.NotFoundProblem;
import com.cowork.booking.common.ApiErrorSchemas.ValidationProblem;
import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.common.AppConstants.Paging;
import com.cowork.booking.space.dto.SpaceFilter;
import com.cowork.booking.space.dto.SpaceRequest;
import com.cowork.booking.space.dto.SpaceResponse;
import com.cowork.booking.space.service.SpaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping(Api.SPACES)
@Tag(name = ApiDocs.Tags.SPACES)
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    @GetMapping
    @Operation(operationId = "listSpaces", summary = "Listar espacios",
            description = "Lista paginada de espacios activos. Filtros opcionales combinados con AND. "
                    + "Página por defecto 20 (máximo 100), ordenada por nombre.")
    @ApiResponse(responseCode = "200", description = "Página de espacios")
    @ApiResponse(responseCode = "400", description = "Filtro, paginación u ordenamiento inválido",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ValidationProblem.class)))
    public PagedModel<SpaceResponse> findAll(@ParameterObject @Valid SpaceFilter filter,
                                             @ParameterObject @PageableDefault(size = Paging.DEFAULT_SIZE, sort = Paging.SPACE_DEFAULT_SORT)
                                             Pageable pageable) {
        return new PagedModel<>(spaceService.findAll(filter, pageable));
    }

    @GetMapping(Api.SPACE_ID)
    @Operation(operationId = "getSpaceById", summary = "Obtener un espacio",
            description = "Devuelve un espacio activo. Los desactivados se informan como no encontrados.")
    @ApiResponse(responseCode = "200", description = "El espacio")
    @ApiResponse(responseCode = "404", description = "Espacio no encontrado",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = NotFoundProblem.class)))
    public SpaceResponse findById(@Parameter(description = "Id del espacio", example = "1") @PathVariable Long spaceId) {
        return spaceService.findById(spaceId);
    }

    @PostMapping
    @Operation(operationId = "createSpace", summary = "Crear un espacio",
            description = "El nombre debe ser único entre los espacios activos (sin distinguir mayúsculas).")
    @ApiResponse(responseCode = "201", description = "Espacio creado",
            headers = @Header(name = HttpHeaders.LOCATION, description = "URL del nuevo espacio", schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ValidationProblem.class)))
    @ApiResponse(responseCode = "409", description = "Nombre en uso por otro espacio activo",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ConflictProblem.class)))
    public ResponseEntity<SpaceResponse> create(@Valid @RequestBody SpaceRequest request) {
        SpaceResponse created = spaceService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path(Api.SPACE_ID).buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping(Api.SPACE_ID)
    @Operation(operationId = "updateSpace", summary = "Reemplazar un espacio",
            description = "Reemplaza todos los datos editables de un espacio activo.")
    @ApiResponse(responseCode = "200", description = "Espacio actualizado")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ValidationProblem.class)))
    @ApiResponse(responseCode = "404", description = "Espacio no encontrado",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = NotFoundProblem.class)))
    @ApiResponse(responseCode = "409", description = "Nombre en uso por otro espacio activo, o modificación concurrente",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = ConflictProblem.class)))
    public SpaceResponse update(@Parameter(description = "Id del espacio", example = "1") @PathVariable Long spaceId,
                                @Valid @RequestBody SpaceRequest request) {
        return spaceService.update(spaceId, request);
    }

    @DeleteMapping(Api.SPACE_ID)
    @Operation(operationId = "deleteSpace", summary = "Desactivar un espacio",
            description = "Borrado lógico: deja de aparecer y libera su nombre, pero se conserva para reservas y reportes.")
    @ApiResponse(responseCode = "204", description = "Espacio desactivado")
    @ApiResponse(responseCode = "404", description = "Espacio no encontrado",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = NotFoundProblem.class)))
    public ResponseEntity<Void> delete(@Parameter(description = "Id del espacio", example = "1") @PathVariable Long spaceId) {
        spaceService.delete(spaceId);
        return ResponseEntity.noContent().build();
    }
}
