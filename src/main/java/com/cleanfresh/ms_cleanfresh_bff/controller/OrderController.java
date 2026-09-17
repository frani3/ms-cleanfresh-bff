package com.cleanfresh.ms_cleanfresh_bff.controller;

import com.cleanfresh.ms_cleanfresh_bff.dto.OrderResponse;
import com.cleanfresh.ms_cleanfresh_bff.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public List<OrderResponse> getAll() {
        return orderService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasAnyRole('Admin', 'Operador')")
    public List<OrderResponse> getByEstado(@PathVariable String estado) {
        return orderService.getByEstado(estado);
    }
}
