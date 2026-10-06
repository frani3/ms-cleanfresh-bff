package com.cleanfresh.ms_cleanfresh_bff.controller;

import com.cleanfresh.ms_cleanfresh_bff.dto.BranchReportResponse;
import com.cleanfresh.ms_cleanfresh_bff.service.ReportesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Republica los reportes de ms-cleanfresh-reportes. Solo Admin: es la
 * pestaña "Analítica por sucursal" del panel.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    private final ReportesService reportesService;

    public ReportesController(ReportesService reportesService) {
        this.reportesService = reportesService;
    }

    @GetMapping
    @PreAuthorize("hasRole('Admin')")
    public List<BranchReportResponse> getAll() {
        return reportesService.getAll();
    }
}
