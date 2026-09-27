package com.cowork.booking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RoleResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "RECEPTION") String name,
        @Schema(example = "Recepción: consulta espacios y reservas") String description,
        @Schema(description = "Rol de sistema (ADMIN, USER): no se puede eliminar") boolean system,
        @Schema(example = "[\"RESERVATION_READ_ALL\", \"SPACE_READ\"]") List<String> permissions
) {
}
