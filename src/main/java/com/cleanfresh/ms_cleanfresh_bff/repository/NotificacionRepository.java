package com.cleanfresh.ms_cleanfresh_bff.repository;

import com.cleanfresh.ms_cleanfresh_bff.dto.NotificacionResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Llama a ms-cleanfresh-notificaciones (http://localhost:8083 por defecto).
 * Ese servicio filtra por el destinatario que se le indique: decidir cuál
 * corresponde a cada rol es responsabilidad del BFF.
 */
@Repository
public class NotificacionRepository {

    private final RestClient restClient;

    public NotificacionRepository(@Qualifier("notificacionesRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** cliente y sucursal son excluyentes; si ambos son null devuelve todas. */
    public List<NotificacionResponse> find(String cliente, String sucursal) {
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/notificaciones");
                    if (cliente != null) {
                        uriBuilder.queryParam("cliente", "{cliente}");
                    }
                    if (sucursal != null) {
                        uriBuilder.queryParam("sucursal", "{sucursal}");
                    }
                    return uriBuilder.build(filtros(cliente, sucursal));
                })
                .retrieve()
                .body(new ParameterizedTypeReference<List<NotificacionResponse>>() {
                });
    }

    public void marcarLeidas(String cliente, String sucursal) {
        restClient.put()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/notificaciones/leidas");
                    if (cliente != null) {
                        uriBuilder.queryParam("cliente", "{cliente}");
                    }
                    if (sucursal != null) {
                        uriBuilder.queryParam("sucursal", "{sucursal}");
                    }
                    return uriBuilder.build(filtros(cliente, sucursal));
                })
                .retrieve()
                .toBodilessEntity();
    }

    private java.util.Map<String, String> filtros(String cliente, String sucursal) {
        java.util.Map<String, String> valores = new java.util.HashMap<>();
        if (cliente != null) {
            valores.put("cliente", cliente);
        }
        if (sucursal != null) {
            valores.put("sucursal", sucursal);
        }
        return valores;
    }
}
