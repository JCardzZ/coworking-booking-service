package com.cowork.booking.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import org.wiremock.integrations.testcontainers.WireMockContainer;

/** Real Postgres and the WireMock payment provider, loaded with the same mappings used by docker compose. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:17-alpine");
    }

    @Bean
    WireMockContainer paymentProvider() {
        return new WireMockContainer("wiremock/wiremock:3.13.2")
                .withCopyFileToContainer(MountableFile.forHostPath("wiremock/mappings"), "/home/wiremock/mappings");
    }

    @Bean
    DynamicPropertyRegistrar paymentServiceUrl(WireMockContainer paymentProvider) {
        return registry -> registry.add("payment.service.url", paymentProvider::getBaseUrl);
    }
}
