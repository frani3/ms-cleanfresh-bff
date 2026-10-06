package com.cleanfresh.ms_cleanfresh_bff.repository;

import com.cleanfresh.ms_cleanfresh_bff.dto.BranchReportResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Llama a ms-cleanfresh-reportes (http://localhost:8084 por defecto).
 */
@Repository
public class ReportesRepository {

    private final RestClient restClient;

    public ReportesRepository(@Qualifier("reportesRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<BranchReportResponse> findAll() {
        return restClient.get()
                .uri("/api/reportes")
                .retrieve()
                .body(new ParameterizedTypeReference<List<BranchReportResponse>>() {
                });
    }
}
