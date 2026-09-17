package com.cleanfresh.ms_cleanfresh_bff.controller;

import com.cleanfresh.ms_cleanfresh_bff.dto.ServiceResponse;
import com.cleanfresh.ms_cleanfresh_bff.service.CatalogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Republica el catalogo de servicios desde ms-cleanfresh-catalog.
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    public List<ServiceResponse> getAll() {
        return catalogService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    public ServiceResponse getById(@PathVariable Long id) {
        return catalogService.getById(id);
    }

    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    public List<ServiceResponse> getDisponibles() {
        return catalogService.getDisponibles();
    }
}
