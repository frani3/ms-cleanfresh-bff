package com.cleanfresh.ms_cleanfresh_bff.controller;

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
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    public List<OrderResponse> getAll(
            JwtAuthenticationToken authentication,
            @RequestParam(required = false) String sucursal) {
        return orderService.getAll(authentication, sucursal);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    public OrderResponse getById(@PathVariable Long id, JwtAuthenticationToken authentication) {
        return orderService.getById(id, authentication);
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasAnyRole('Admin', 'Operador')")
    public List<OrderResponse> getByEstado(
            @PathVariable String estado,
            JwtAuthenticationToken authentication,
            @RequestParam(required = false) String sucursal) {
        return orderService.getByEstado(estado, authentication, sucursal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('Cliente', 'Admin')")
    public OrderResponse create(@RequestBody OrderRequest request, JwtAuthenticationToken authentication) {
        return orderService.create(authentication, request);
    }

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
