package com.cleanfresh.ms_cleanfresh_bff.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

/**
 * Llama a ms-cleanfresh-kafka-admin (http://localhost:8087 por defecto). Reenvía la solicitud tal cual y devuelve la respuesta
 * tal cual, con su código: lo que el administrador contesta (409 por un tópico existente, 404 por un grupo inexistente...) le sirve
 * al Admin y no debe convertirse en un 500 del BFF.
 */
@Repository
public class KafkaAdminRepository {

    private final RestClient restClient;

    public KafkaAdminRepository(@Qualifier("kafkaAdminRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** @param rutaYConsulta lo que va después de {@code /api/kafka}, con su query string (p. ej. {@code /topics/x?force=true}) */
    public ResponseEntity<String> reenviar(HttpMethod metodo, String rutaYConsulta, String cuerpo) {
        return ReenvioHttp.reenviar(restClient, metodo, "/api/kafka" + rutaYConsulta, cuerpo, "El administrador de Kafka no responde");
    }
}
