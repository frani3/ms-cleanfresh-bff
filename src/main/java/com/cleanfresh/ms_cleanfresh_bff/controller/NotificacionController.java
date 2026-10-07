package com.cleanfresh.ms_cleanfresh_bff.controller;

import com.cleanfresh.ms_cleanfresh_bff.dto.NotificacionResponse;
import com.cleanfresh.ms_cleanfresh_bff.service.NotificacionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Avisos de ms-cleanfresh-notificaciones, filtrados según el rol de quien
 * pregunta. No recibe un "cliente" por parámetro a propósito: ese dato sale del
 * token.
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    public List<NotificacionResponse> getAll(
            JwtAuthenticationToken authentication,
            @RequestParam(required = false) String sucursal) {
        return notificacionService.listar(authentication, sucursal);
    }

    @PostMapping("/leidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    public void marcarLeidas(
            JwtAuthenticationToken authentication,
            @RequestParam(required = false) String sucursal) {
        notificacionService.marcarLeidas(authentication, sucursal);
    }
}
