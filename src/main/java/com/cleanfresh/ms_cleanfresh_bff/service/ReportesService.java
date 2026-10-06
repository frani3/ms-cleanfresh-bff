package com.cleanfresh.ms_cleanfresh_bff.service;

import com.cleanfresh.ms_cleanfresh_bff.dto.BranchReportResponse;
import com.cleanfresh.ms_cleanfresh_bff.repository.ReportesRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportesService {

    private final ReportesRepository reportesRepository;

    public ReportesService(ReportesRepository reportesRepository) {
        this.reportesRepository = reportesRepository;
    }

    public List<BranchReportResponse> getAll() {
        return reportesRepository.findAll();
    }
}
