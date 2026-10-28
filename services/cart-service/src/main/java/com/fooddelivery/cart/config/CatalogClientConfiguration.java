package com.fooddelivery.cart.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CatalogClientConfiguration {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient catalogRestClient(
            RestClient.Builder builder,
            @Value("${catalog.service.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
