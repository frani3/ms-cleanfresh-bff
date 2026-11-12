package com.cleanfresh.ms_cleanfresh_bff.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

/**
 * Llama a ms-cleanfresh-reportes (http://localhost:8084 por defecto). Solo lectura: reenvía la consulta tal cual y devuelve la
 * respuesta tal cual, con su código (un {@code range} inválido sale como 400, no como un 500 del BFF).
 */
@Repository
public class ReportesRepository {

    private final RestClient restClient;

    public ReportesRepository(@Qualifier("reportesRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** @param rutaYConsulta lo que va después de {@code /api/reportes}, con su query string (p. ej. {@code /kpis?range=last24h}) */
    public ResponseEntity<String> consultar(String rutaYConsulta) {
        return ReenvioHttp.reenviar(restClient, HttpMethod.GET, "/api/reportes" + rutaYConsulta, null, "El servicio de reportes no responde");
    }
}
