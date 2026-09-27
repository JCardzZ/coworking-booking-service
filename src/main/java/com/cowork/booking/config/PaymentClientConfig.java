package com.cowork.booking.config;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PaymentClientConfig {

    @Bean
    RestClient paymentRestClient(RestClient.Builder builder, PaymentServiceProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(properties.connectTimeout())
                .withReadTimeout(properties.readTimeout());
        return builder
                .baseUrl(properties.url())
                // plain HTTP/1.1: the JDK client tries an HTTP/2 upgrade that some providers (and WireMock) reset
                .requestFactory(ClientHttpRequestFactoryBuilder.simple().build(settings))
                .build();
    }
}
