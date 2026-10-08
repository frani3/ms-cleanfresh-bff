package com.cleanfresh.ms_cleanfresh_bff.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient dedicado por microservicio downstream, con su base-url leida
 * desde application.yaml (microservices.<servicio>.base-url).
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

    @Bean
    @Qualifier("reportesRestClient")
    public RestClient reportesRestClient(@Value("${microservices.reportes.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    @Qualifier("auditoriaRestClient")
    public RestClient auditoriaRestClient(@Value("${microservices.auditoria.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    // Spec 032: dominio del Hosted UI de Cognito, para pedir /oauth2/userInfo. Puede venir
    // vacío (el BFF funciona igual y las ordenes se muestran con el username).
    @Bean
    @Qualifier("cognitoRestClient")
    public RestClient cognitoRestClient(@Value("${cognito.domain:}") String domain) {
        RestClient.Builder builder = RestClient.builder();
        if (domain != null && !domain.isBlank()) {
            builder.baseUrl(domain);
        }
        return builder.build();
    }

    @Bean
    @Qualifier("notificacionesRestClient")
    public RestClient notificacionesRestClient(@Value("${microservices.notificaciones.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
