package com.cleanfresh.ms_cleanfresh_bff.repository;

import com.cleanfresh.ms_cleanfresh_bff.dto.OrderResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Llama a ms-cleanfresh-orders (http://localhost:8081 por defecto).
 * Asume que expone las mismas rutas relativas que el BFF republica
 * (/api/orders, /api/orders/{id}, /api/orders/estado/{estado});
 * ajustar si el contrato real del microservicio difiere.
 */
@Repository
public class OrderRepository {

    private final RestClient restClient;

    public OrderRepository(@Qualifier("ordersRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<OrderResponse> findAll() {
        return restClient.get()
                .uri("/api/orders")
                .retrieve()
                .body(new ParameterizedTypeReference<List<OrderResponse>>() {
                });
    }

    public OrderResponse findById(Long id) {
        return restClient.get()
                .uri("/api/orders/{id}", id)
                .retrieve()
                .body(OrderResponse.class);
    }

    public List<OrderResponse> findByEstado(String estado) {
        return restClient.get()
                .uri("/api/orders/estado/{estado}", estado)
                .retrieve()
                .body(new ParameterizedTypeReference<List<OrderResponse>>() {
                });
    }
}
