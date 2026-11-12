package com.cleanfresh.ms_cleanfresh_bff.controller;

import com.cleanfresh.ms_cleanfresh_bff.repository.KafkaAdminRepository;
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
 * Puerta del Admin al administrador de Kafka (Spec 044): clúster, tópicos, configuraciones, consumer groups y métricas.
 * Solo rol Admin. Igual que el de RabbitMQ, el BFF no interpreta la solicitud: la valida por rol, comprueba que apunte a una
 * de las rutas del administrador y la reenvía.
 */
@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    // Solo estas rutas existen en el administrador; cualquier otra no se reenvía.
    private static final String NOMBRE = "[A-Za-z0-9._-]{1,249}";
    private static final Pattern RUTA_PERMITIDA = Pattern.compile(
            "/(cluster|metricas|topologia/base|topics"
                    + "|topics/" + NOMBRE
                    + "|topics/" + NOMBRE + "/config"
                    + "|topics/" + NOMBRE + "/config/" + NOMBRE
                    + "|topics/" + NOMBRE + "/partitions"
                    + "|groups"
                    + "|groups/" + NOMBRE
                    + "|groups/" + NOMBRE + "/offsets)");

    private final KafkaAdminRepository kafkaAdminRepository;

    public KafkaAdminController(KafkaAdminRepository kafkaAdminRepository) {
        this.kafkaAdminRepository = kafkaAdminRepository;
    }

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<String> reenviar(HttpServletRequest request, @RequestBody(required = false) String cuerpo) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length() + "/api/kafka".length());
        if (!RUTA_PERMITIDA.matcher(ruta).matches()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"detail\":\"Ruta inexistente\"}");
        }
        String consulta = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        String cuerpoUtil = cuerpo == null || cuerpo.isBlank() ? null : cuerpo;
        return kafkaAdminRepository.reenviar(HttpMethod.valueOf(request.getMethod()), ruta + consulta, cuerpoUtil);
    }
}
