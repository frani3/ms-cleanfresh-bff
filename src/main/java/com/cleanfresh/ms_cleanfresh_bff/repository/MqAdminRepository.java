package com.cleanfresh.ms_cleanfresh_bff.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Llama a ms-cleanfresh-mq-admin (http://localhost:8086 por defecto). Reenvía la solicitud tal cual y
 * devuelve la respuesta tal cual, con su código: lo que el administrador contesta (400 por un nombre
 * inválido, 404 por algo que no existe...) le sirve al Admin y no debe convertirse en un 500 del BFF.
 */
@Repository
public class MqAdminRepository {

    private final RestClient restClient;

    public MqAdminRepository(@Qualifier("mqAdminRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** @param rutaYConsulta lo que va después de {@code /api/mq}, con su query string (p. ej. {@code /queues/q.x?conDlq=true}) */
    public ResponseEntity<String> reenviar(HttpMethod metodo, String rutaYConsulta, String cuerpo) {
        try {
            RestClient.RequestBodySpec solicitud = restClient.method(metodo).uri("/api/mq" + rutaYConsulta);
            if (cuerpo != null && !cuerpo.isBlank()) {
                solicitud.contentType(MediaType.APPLICATION_JSON).body(cuerpo);
            }
            // exchange no lanza excepción por un 4xx/5xx: la respuesta se devuelve como llegó
            return solicitud.exchange((req, res) -> ResponseEntity.status(res.getStatusCode())
                    .contentType(res.getHeaders().getContentType() == null ? MediaType.APPLICATION_JSON : res.getHeaders().getContentType())
                    .body(new String(res.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).contentType(MediaType.APPLICATION_JSON)
                    .body("{\"detail\":\"El administrador de RabbitMQ no responde\"}");
        }
    }
}
