package com.cleanfresh.ms_cleanfresh_bff.dto;

/**
 * Espejo del recurso Order expuesto por ms-cleanfresh-orders. {@code fechaHora} es el instante de creación
 * (ISO-8601, UTC) y es null en las órdenes anteriores a la Spec 036, que solo tienen {@code fecha}.
 */
public record OrderResponse(
        Long id,
        String numeroOrden,
        String cliente,
        String servicio,
        String estado,
        String fecha,
        Double total,
        String sucursal,
        String clienteNombre,
        String fechaHora
) {

    public OrderResponse(Long id, String numeroOrden, String cliente, String servicio, String estado,
                         String fecha, Double total, String sucursal, String clienteNombre) {
        this(id, numeroOrden, cliente, servicio, estado, fecha, total, sucursal, clienteNombre, null);
    }

    public OrderResponse(Long id, String numeroOrden, String cliente, String servicio, String estado,
                         String fecha, Double total, String sucursal) {
        this(id, numeroOrden, cliente, servicio, estado, fecha, total, sucursal, null, null);
    }
}
