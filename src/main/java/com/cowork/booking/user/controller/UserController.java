package com.cowork.booking.user.controller;

import com.cowork.booking.common.ApiDocs;
import com.cowork.booking.common.ApiErrorSchemas.NotFoundProblem;
import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.user.dto.UserResponse;
import com.cowork.booking.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Api.USERS)
@Tag(name = ApiDocs.Tags.USERS)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping(Api.ME)
    @Operation(operationId = "getCurrentUser", summary = "Mi perfil",
            description = "Devuelve el usuario del token (claim sub); no recibe ningún id.")
    @ApiResponse(responseCode = "200", description = "Usuario autenticado")
    @ApiResponse(responseCode = "404", description = "El usuario del token ya no existe",
            content = @Content(mediaType = ApiDocs.PROBLEM_JSON, schema = @Schema(implementation = NotFoundProblem.class)))
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.findById(Long.valueOf(jwt.getSubject()));
    }
}
