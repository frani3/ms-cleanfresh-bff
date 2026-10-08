package com.cleanfresh.ms_cleanfresh_bff.dto;

/**
 * Lo que el BFF reenvía a ms-cleanfresh-orders para crear un pedido:
 * mismo contrato que su OrderRequest (cliente ya resuelto desde el JWT).
 * clienteNombre es el nombre legible, que el BFF le pide a Cognito (Spec 032).
 */
public record OrderCreateRequest(
        String cliente,
        String servicio,
        Double total,
        String sucursal,
        String clienteNombre
) {
}
