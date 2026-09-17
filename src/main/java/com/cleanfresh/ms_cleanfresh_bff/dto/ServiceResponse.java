package com.cleanfresh.ms_cleanfresh_bff.dto;

import java.math.BigDecimal;

/**
 * Espejo del recurso Service (item de catalogo) expuesto por ms-cleanfresh-catalog.
 * Ajustar los campos si el contrato real del microservicio difiere.
 */
public record ServiceResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        boolean disponible,
        String categoria
) {
}
