package com.cowork.booking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI bookingOpenApi(ObjectProvider<BuildProperties> buildProperties) {
        String version = buildProperties.stream().map(BuildProperties::getVersion).findFirst().orElse("dev");

        return new OpenAPI()
                .info(new Info()
                        .title("Coworking Booking Service")
                        .description("Microservicio de gestión de reservas de espacios de coworking.")
                        .version(version))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .tags(List.of(
                        new Tag().name("Spaces").description("Gestión de espacios de coworking"),
                        new Tag().name("Users").description("Gestión de usuarios y roles"),
                        new Tag().name("Reservations").description("Reservas de espacios y reporte de ocupación")));
    }
}
