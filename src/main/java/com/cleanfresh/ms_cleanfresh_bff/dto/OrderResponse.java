package com.cleanfresh.ms_cleanfresh_bff.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Espejo del recurso Order expuesto por ms-cleanfresh-orders.
 * Ajustar los campos si el contrato real del microservicio difiere.
 */
public record OrderResponse(
        Long id,
        String clienteId,
        String clienteNombre,
        String estado,
        Instant fechaCreacion,
        BigDecimal total
) {
}
