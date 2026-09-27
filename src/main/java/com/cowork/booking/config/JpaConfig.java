package com.cowork.booking.config;

import com.cowork.booking.common.CurrentUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import java.util.Optional;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@Configuration
// Fills created/updated at and by of AuditableEntity
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
// Stable JSON for paged responses (content + page)
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class JpaConfig {

    @Bean
    AuditorAware<String> auditorAware() {
        return () -> Optional.of(CurrentUser.email());
    }
}
