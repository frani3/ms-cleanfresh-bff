package com.cleanfresh.ms_cleanfresh_bff.dto;

import java.util.Map;

/**
 * Espejo del recurso Service (item de catalogo) expuesto por ms-cleanfresh-catalog.
 */
public record ServiceResponse(
        Long id,
        String nombre,
        String descripcion,
        Double precio,
        Double duracionHoras,
        Boolean disponible,
        Map<String, Boolean> sucursales
) {
}
