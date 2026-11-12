package com.cleanfresh.ms_cleanfresh_bff.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.cleanfresh.ms_cleanfresh_bff.dto.NotificacionResponse;
import com.cleanfresh.ms_cleanfresh_bff.service.NotificacionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@Tag(name = "Notificaciones", description = "Avisos filtrados por rol: el Cliente los suyos, el Operador los de su sucursal en turno y el Admin todos, en solo lectura.")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @Operation(summary = "Lista los avisos que le corresponden al rol")
    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    public List<NotificacionResponse> getAll(
            JwtAuthenticationToken authentication,
            @RequestParam(required = false) String sucursal) {
        return notificacionService.listar(authentication, sucursal);
    }

    @Operation(summary = "Marca como leídos todos los avisos del usuario")
    @PostMapping("/leidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    public void marcarLeidas(
            JwtAuthenticationToken authentication,
            @RequestParam(required = false) String sucursal) {
        notificacionService.marcarLeidas(authentication, sucursal);
    }

    /** Marca UN aviso como leído (Spec 034). Solo Operador y Cliente: el Admin es solo lectura (403). */
    @Operation(summary = "Marca un aviso como leído (404 si es ajeno o no existe; 403 al Admin)")
    @PostMapping("/{id}/leida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('Operador', 'Cliente')")
    public void marcarLeida(
            JwtAuthenticationToken authentication,
            @PathVariable Long id,
            @RequestParam(required = false) String sucursal) {
        notificacionService.marcarLeida(authentication, id, sucursal);
    }
}
