package com.cleanfresh.ms_cleanfresh_bff.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient dedicado por microservicio downstream, con su base-url leida
 * desde application.yaml (microservices.orders/catalog.base-url).
 */
@Configuration
public class RestClientConfig {

    @Bean
    @Qualifier("ordersRestClient")
    public RestClient ordersRestClient(@Value("${microservices.orders.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    @Qualifier("catalogRestClient")
    public RestClient catalogRestClient(@Value("${microservices.catalog.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
