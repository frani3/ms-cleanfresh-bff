package com.cleanfresh.ms_cleanfresh_bff.dto;

/**
 * Lo que el BFF reenvía a ms-cleanfresh-orders para crear un pedido:
 * mismo contrato que su OrderRequest (cliente ya resuelto desde el JWT).
 */
public record OrderCreateRequest(
        String cliente,
        String servicio,
        Double total,
        String sucursal
) {
}
