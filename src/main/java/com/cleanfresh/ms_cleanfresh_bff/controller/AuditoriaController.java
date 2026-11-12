package com.cleanfresh.ms_cleanfresh_bff.controller;

import com.cleanfresh.ms_cleanfresh_bff.repository.AuditoriaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

/**
 * Republica la auditoría de ms-cleanfresh-auditoria (Spec 044): la línea de tiempo (con filtros por usuario, tipo, orden y fechas) y la
 * trazabilidad de una orden. Solo lectura y solo Admin. El BFF no interpreta la consulta: la valida por rol, comprueba que apunte a
 * una de las rutas del servicio y la reenvía con su query string.
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    // Solo estas rutas existen en el servicio; cualquier otra no se reenvía. La raíz es la línea de tiempo.
    private static final Pattern RUTA_PERMITIDA = Pattern.compile("|/tipos|/ordenes/[A-Za-z0-9._-]{1,60}");

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaController(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @GetMapping({"", "/**"})
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<String> consultar(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length() + "/api/auditoria".length());
        if (!RUTA_PERMITIDA.matcher(ruta).matches()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"detail\":\"Ruta inexistente\"}");
        }
        String consulta = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        return auditoriaRepository.consultar(ruta + consulta);
    }
}
