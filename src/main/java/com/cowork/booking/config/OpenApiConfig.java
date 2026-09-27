package com.cowork.booking.config;

import com.cowork.booking.common.ApiDocs;
import com.cowork.booking.common.ApiDocs.Examples;
import com.cowork.booking.common.ApiDocs.Tags;
import com.cowork.booking.common.ApiErrorSchemas;
import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Messages.Common;
import com.cowork.booking.common.AppConstants.Problem;
import com.cowork.booking.common.AppConstants.Security;
import com.cowork.booking.common.AppConstants.Tracing;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Configuration
public class OpenApiConfig {

    private static final String PROBLEM_SCHEMA_REF = Components.COMPONENTS_SCHEMAS_REF + ApiErrorSchemas.PROBLEM_SCHEMA;

    @Bean
    OpenAPI bookingOpenApi(ObjectProvider<BuildProperties> buildProperties) {
        String version = buildProperties.stream().map(BuildProperties::getVersion).findFirst().orElse("dev");

        return new OpenAPI()
                .info(new Info()
                        .title("Coworking Booking Service")
                        .description("Microservicio de gestión de reservas de espacios de coworking.")
                        .version(version))
                .components(new Components().addSecuritySchemes(Security.BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(Security.BEARER_SCHEME))
                .tags(List.of(
                        new Tag().name(Tags.SPACES).description("Gestión de espacios de coworking"),
                        new Tag().name(Tags.AUTH).description("Registro e inicio de sesión (JWT)"),
                        new Tag().name(Tags.USERS).description("Perfil del usuario autenticado"),
                        new Tag().name(Tags.ADMIN).description("Administración de usuarios, roles y permisos (RBAC)")));
    }

    /** Adds 401/403 (secured operations) and 500 to every operation. */
    @Bean
    OpenApiCustomizer commonErrorResponses() {
        return openApi -> {
            ModelConverters.getInstance().readAll(ApiErrorSchemas.Problem.class)
                    .forEach(openApi.getComponents()::addSchemas);
            openApi.getPaths().values().stream()
                    .flatMap(path -> path.readOperations().stream())
                    .forEach(OpenApiConfig::addCommonResponses);
        };
    }

    /** Shows domain paths; the {@code /api/v1} prefix goes to the server URL. */
    @Bean
    OpenApiCustomizer apiBasePathAsServer(ServerProperties serverProperties) {
        return openApi -> {
            String contextPath = Objects.requireNonNullElse(serverProperties.getServlet().getContextPath(), "");
            Paths domainPaths = new Paths();
            openApi.getPaths().forEach((path, item) ->
                    domainPaths.addPathItem(path.startsWith(Api.BASE_PATH) ? path.substring(Api.BASE_PATH.length()) : path, item));
            openApi.setPaths(domainPaths);
            openApi.setServers(List.of(new Server()
                    .url(contextPath + Api.BASE_PATH)
                    .description("API v1")));
        };
    }

    private static void addCommonResponses(Operation operation) {
        operation.addParametersItem(new HeaderParameter()
                .name(Tracing.CORRELATION_ID_HEADER)
                .description("Id de correlación; si no se envía se genera uno (máx. 64: letras, números y guiones)")
                .required(false)
                .schema(new StringSchema().example(Examples.TRACE_ID)));
        ApiResponses responses = operation.getResponses();
        boolean secured = operation.getSecurity() == null || !operation.getSecurity().isEmpty();
        if (secured) {
            responses.putIfAbsent(code(HttpStatus.UNAUTHORIZED), errorResponse(HttpStatus.UNAUTHORIZED, Common.UNAUTHORIZED_TITLE,
                    Common.UNAUTHORIZED_DETAIL, ErrorCodes.UNAUTHORIZED));
            responses.putIfAbsent(code(HttpStatus.FORBIDDEN), errorResponse(HttpStatus.FORBIDDEN, Common.FORBIDDEN_TITLE,
                    Common.FORBIDDEN_DETAIL, ErrorCodes.FORBIDDEN));
        }
        responses.putIfAbsent(code(HttpStatus.INTERNAL_SERVER_ERROR), errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, Common.INTERNAL_ERROR_TITLE,
                Common.INTERNAL_ERROR_DETAIL, ErrorCodes.INTERNAL_ERROR));
    }

    private static String code(HttpStatus status) {
        return String.valueOf(status.value());
    }

    private static ApiResponse errorResponse(HttpStatus status, String title, String detail, String code) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put(Problem.TYPE, Examples.PROBLEM_TYPE_BASE + Problem.slug(code));
        example.put(Problem.TITLE, title);
        example.put(Problem.STATUS, status.value());
        example.put(Problem.DETAIL, detail);
        example.put(Problem.INSTANCE, Examples.API_PATH + "/spaces");
        example.put(Problem.CODE, code);
        example.put(Problem.TIMESTAMP, Examples.TIMESTAMP);
        example.put(Problem.TRACE_ID, Examples.TRACE_ID);
        return new ApiResponse()
                .description(title)
                .content(new Content().addMediaType(ApiDocs.PROBLEM_JSON, new MediaType()
                        .schema(new Schema<>().$ref(PROBLEM_SCHEMA_REF))
                        .example(example)));
    }
}
