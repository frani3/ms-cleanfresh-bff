package com.cleanfresh.ms_cleanfresh_bff.controller;

import com.cleanfresh.ms_cleanfresh_bff.repository.MqAdminRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

/**
 * Puerta del Admin al administrador de RabbitMQ (Spec 033): topología, colas, exchanges, bindings y
 * métricas. Solo rol Admin. El BFF no interpreta la solicitud: la valida por rol, comprueba que apunte a
 * una de las rutas del administrador y la reenvía.
 */
@RestController
@RequestMapping("/api/mq")
public class MqAdminController {

    // Solo estas rutas existen en el administrador; cualquier otra no se reenvía.
    private static final Pattern RUTA_PERMITIDA = Pattern.compile(
            "/(topologia(/base)?|metricas|bindings|queues/[A-Za-z0-9._-]{1,100}|exchanges/[A-Za-z0-9._-]{1,100})");

    private final MqAdminRepository mqAdminRepository;

    public MqAdminController(MqAdminRepository mqAdminRepository) {
        this.mqAdminRepository = mqAdminRepository;
    }

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE})
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<String> reenviar(HttpServletRequest request, @RequestBody(required = false) String cuerpo) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length() + "/api/mq".length());
        if (!RUTA_PERMITIDA.matcher(ruta).matches()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"detail\":\"Ruta inexistente\"}");
        }
        String consulta = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        String cuerpoUtil = cuerpo == null || cuerpo.isBlank() ? null : cuerpo;
        return mqAdminRepository.reenviar(HttpMethod.valueOf(request.getMethod()), ruta + consulta, cuerpoUtil);
    }
}
