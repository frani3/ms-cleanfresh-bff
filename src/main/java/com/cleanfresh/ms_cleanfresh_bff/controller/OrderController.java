package com.cleanfresh.ms_cleanfresh_bff.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.cleanfresh.ms_cleanfresh_bff.dto.EstadoRequest;
import com.cleanfresh.ms_cleanfresh_bff.dto.OrderRequest;
import com.cleanfresh.ms_cleanfresh_bff.dto.OrderResponse;
import com.cleanfresh.ms_cleanfresh_bff.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Republica ordenes de lavanderia desde ms-cleanfresh-orders.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Órdenes", description = "Órdenes según el rol: el Cliente solo las suyas, el Operador las de su sucursal en turno y el Admin todas.")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Lista las órdenes que le corresponden al rol")
    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    public List<OrderResponse> getAll(
            JwtAuthenticationToken authentication,
            @RequestParam(required = false) String sucursal) {
        return orderService.getAll(authentication, sucursal);
    }

    @Operation(summary = "Obtiene una orden por su id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    public OrderResponse getById(@PathVariable Long id, JwtAuthenticationToken authentication) {
        return orderService.getById(id, authentication);
    }

    @Operation(summary = "Lista las órdenes de un estado (Admin y Operador)")
    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasAnyRole('Admin', 'Operador')")
    public List<OrderResponse> getByEstado(
            @PathVariable String estado,
            JwtAuthenticationToken authentication,
            @RequestParam(required = false) String sucursal) {
        return orderService.getByEstado(estado, authentication, sucursal);
    }

    @Operation(summary = "Crea una orden: el cliente sale del token y el nombre legible lo resuelve el BFF con Cognito (Cliente y Admin)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('Cliente', 'Admin')")
    public OrderResponse create(@RequestBody OrderRequest request, JwtAuthenticationToken authentication) {
        return orderService.create(authentication, request);
    }

    @Operation(summary = "Cambia el estado de una orden (Admin y Operador de esa sucursal)")
    @PutMapping("/{numeroOrden}/estado")
    @PreAuthorize("hasAnyRole('Admin', 'Operador')")
    public OrderResponse cambiarEstado(
            @PathVariable String numeroOrden,
            @RequestBody EstadoRequest request,
            JwtAuthenticationToken authentication,
            @RequestParam(required = false) String sucursal) {
        return orderService.cambiarEstado(numeroOrden, request.estado(), authentication, sucursal);
    }
}
