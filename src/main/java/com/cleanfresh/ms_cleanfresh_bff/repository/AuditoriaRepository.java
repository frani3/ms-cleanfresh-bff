package com.cleanfresh.ms_cleanfresh_bff.repository;

import com.cleanfresh.ms_cleanfresh_bff.dto.AuditEntryResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Llama a ms-cleanfresh-auditoria (http://localhost:8085 por defecto).
 */
@Repository
public class AuditoriaRepository {

    private final RestClient restClient;

    public AuditoriaRepository(@Qualifier("auditoriaRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<AuditEntryResponse> findAll() {
        return restClient.get()
                .uri("/api/auditoria")
                .retrieve()
                .body(new ParameterizedTypeReference<List<AuditEntryResponse>>() {
                });
    }
}
