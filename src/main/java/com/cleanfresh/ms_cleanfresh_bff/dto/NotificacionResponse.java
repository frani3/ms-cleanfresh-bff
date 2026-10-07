package com.cleanfresh.ms_cleanfresh_bff.dto;

/**
 * Espejo del aviso que expone ms-cleanfresh-notificaciones.
 */
public record NotificacionResponse(
        Long id,
        String tipo,
        String destinatarioTipo,
        String destinatario,
        String numeroOrden,
        String mensaje,
        String fechaHora,
        boolean leida
) {
}
