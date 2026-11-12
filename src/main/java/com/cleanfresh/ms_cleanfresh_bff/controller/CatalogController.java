package com.cleanfresh.ms_cleanfresh_bff.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Catálogo", description = "Servicios de lavandería y su disponibilidad por sucursal. Solo lectura.")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(summary = "Lista los servicios del catálogo")
    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    public List<ServiceResponse> getAll() {
        return catalogService.getAll();
    }

    @Operation(summary = "Obtiene un servicio por su id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    public ServiceResponse getById(@PathVariable Long id) {
        return catalogService.getById(id);
    }

    @Operation(summary = "Lista los servicios disponibles al menos en una sucursal")
    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    public List<ServiceResponse> getDisponibles() {
        return catalogService.getDisponibles();
    }
}
