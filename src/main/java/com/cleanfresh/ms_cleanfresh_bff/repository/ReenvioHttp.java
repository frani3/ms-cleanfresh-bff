package com.cleanfresh.ms_cleanfresh_bff.repository;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

/**
 * Reenvía una solicitud a un microservicio y devuelve su respuesta tal cual, con su código: lo que el servicio contesta (400 por un
 * filtro mal escrito, 404 por algo que no existe, 409 por un conflicto...) le sirve a quien llamó y no debe convertirse en un 500 del
 * BFF. Si el servicio no responde, 503. Lo usan los repositorios de los servicios que el BFF solo republica (Kafka, reportes y
 * auditoría).
 */
final class ReenvioHttp {

    private ReenvioHttp() {
    }

    static ResponseEntity<String> reenviar(RestClient cliente, HttpMethod metodo, String uri, String cuerpo, String mensajeSiNoResponde) {
        try {
            RestClient.RequestBodySpec solicitud = cliente.method(metodo).uri(uri);
            if (cuerpo != null && !cuerpo.isBlank()) {
                solicitud.contentType(MediaType.APPLICATION_JSON).body(cuerpo);
            }
            // exchange no lanza excepción por un 4xx/5xx: la respuesta se devuelve como llegó
            return solicitud.exchange((req, res) -> ResponseEntity.status(res.getStatusCode())
                    .contentType(res.getHeaders().getContentType() == null ? MediaType.APPLICATION_JSON : res.getHeaders().getContentType())
                    .body(new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8)));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).contentType(MediaType.APPLICATION_JSON)
                    .body("{\"detail\":\"" + mensajeSiNoResponde + "\"}");
        }
    }
}
