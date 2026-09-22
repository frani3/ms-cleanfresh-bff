package com.cleanfresh.ms_cleanfresh_bff.dto;

/**
 * Lo que manda el frontend al pedir un servicio. `cliente` no viaja acá:
 * el BFF lo completa desde el JWT antes de reenviar a ms-cleanfresh-orders
 * (Spec 025) para no confiar en que el cliente diga de quién es la orden.
 */
public record OrderRequest(
        String servicio,
        Double total,
        String sucursal
) {
}
