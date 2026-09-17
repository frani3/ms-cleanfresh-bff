package com.cleanfresh.ms_cleanfresh_bff.repository;

import com.cleanfresh.ms_cleanfresh_bff.dto.ServiceResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Llama a ms-cleanfresh-catalog (http://localhost:8082 por defecto).
 * Asume que expone las mismas rutas relativas que el BFF republica
 * (/api/catalog, /api/catalog/{id}, /api/catalog/disponibles);
 * ajustar si el contrato real del microservicio difiere.
 */
@Repository
public class CatalogRepository {

    private final RestClient restClient;

    public CatalogRepository(@Qualifier("catalogRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<ServiceResponse> findAll() {
        return restClient.get()
                .uri("/api/catalog")
                .retrieve()
                .body(new ParameterizedTypeReference<List<ServiceResponse>>() {
                });
    }

    public ServiceResponse findById(Long id) {
        return restClient.get()
                .uri("/api/catalog/{id}", id)
                .retrieve()
                .body(ServiceResponse.class);
    }

    public List<ServiceResponse> findDisponibles() {
        return restClient.get()
                .uri("/api/catalog/disponibles")
                .retrieve()
                .body(new ParameterizedTypeReference<List<ServiceResponse>>() {
                });
    }
}
