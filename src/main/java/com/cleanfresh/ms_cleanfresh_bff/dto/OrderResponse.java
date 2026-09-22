package com.cleanfresh.ms_cleanfresh_bff.dto;

/**
 * Espejo del recurso Order expuesto por ms-cleanfresh-orders.
 */
public record OrderResponse(
        Long id,
        String numeroOrden,
        String cliente,
        String servicio,
        String estado,
        String fecha,
        Double total,
        String sucursal
) {
}
