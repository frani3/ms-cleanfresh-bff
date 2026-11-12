package com.cleanfresh.ms_cleanfresh_bff.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.cleanfresh.ms_cleanfresh_bff.repository.ReportesRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

/**
 * Republica los reportes de ms-cleanfresh-reportes (Spec 044): pedidos por sucursal, KPIs, ventas por hora y servicios más
 * pedidos, calculados con los eventos de Kafka. Solo lectura y solo Admin (el panel de analítica). El BFF no interpreta la
 * consulta: la valida por rol, comprueba que apunte a una de las rutas del servicio y la reenvía con su query string.
 */
@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "Reportes de Kafka (KPIs, ventas por hora, lead time, servicios más pedidos). Solo Admin y solo lectura.")
public class ReportesController {

    // Solo estas rutas existen en el servicio; cualquier otra no se reenvía. La raíz son los pedidos por sucursal.
    private static final Pattern RUTA_PERMITIDA = Pattern.compile("|/kpis|/ventas-por-hora|/top-servicios");

    private final ReportesRepository reportesRepository;

    public ReportesController(ReportesRepository reportesRepository) {
        this.reportesRepository = reportesRepository;
    }

    @Operation(summary = "Reenvía la consulta a ms-cleanfresh-reportes: /api/reportes, /kpis, /ventas-por-hora y /top-servicios, con ?range=last1h|last24h|last7d|last30d")
    @GetMapping({"", "/**"})
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<String> consultar(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length() + "/api/reportes".length());
        if (!RUTA_PERMITIDA.matcher(ruta).matches()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"detail\":\"Ruta inexistente\"}");
        }
        String consulta = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        return reportesRepository.consultar(ruta + consulta);
    }
}
