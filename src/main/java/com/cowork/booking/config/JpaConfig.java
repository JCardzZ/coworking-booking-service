package com.cowork.booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@Configuration
// Fills createdAt/updatedAt of AuditableEntity
@EnableJpaAuditing
// Stable JSON for paged responses (content + page)
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class JpaConfig {
}
