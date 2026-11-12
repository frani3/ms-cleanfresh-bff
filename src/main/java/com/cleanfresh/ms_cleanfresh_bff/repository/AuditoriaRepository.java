package com.cleanfresh.ms_cleanfresh_bff.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

/**
 * Llama a ms-cleanfresh-auditoria (http://localhost:8085 por defecto). Solo lectura: reenvía la consulta tal cual y devuelve la
 * respuesta tal cual, con su código (un filtro mal escrito sale como 400 y una orden sin registros como 404).
 */
@Repository
public class AuditoriaRepository {

    private final RestClient restClient;

    public AuditoriaRepository(@Qualifier("auditoriaRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** @param rutaYConsulta lo que va después de {@code /api/auditoria}, con su query string (p. ej. {@code ?usuario=ana&tipo=OrderCreated}) */
    public ResponseEntity<String> consultar(String rutaYConsulta) {
        return ReenvioHttp.reenviar(restClient, HttpMethod.GET, "/api/auditoria" + rutaYConsulta, null, "El servicio de auditoría no responde");
    }
}
