package com.cleanfresh.ms_cleanfresh_bff.controller;

import com.cleanfresh.ms_cleanfresh_bff.dto.AuditEntryResponse;
import com.cleanfresh.ms_cleanfresh_bff.service.AuditoriaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Republica el registro de ms-cleanfresh-auditoria. Solo Admin: es la
 * pestaña "Registro de auditoría" del panel.
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @PreAuthorize("hasRole('Admin')")
    public List<AuditEntryResponse> getAll() {
        return auditoriaService.getAll();
    }
}
