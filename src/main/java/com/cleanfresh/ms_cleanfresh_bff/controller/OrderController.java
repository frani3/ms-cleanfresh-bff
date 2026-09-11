package com.cleanfresh.ms_cleanfresh_bff.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Endpoints CRUD basicos para ordenes de lavanderia.
 * Placeholder en memoria: la logica real debera delegarse a un microservicio downstream.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public Iterable<Order> getAll() {
        return orders.values();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        Order order = orders.get(id);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Order> create(@RequestBody OrderRequest request) {
        long id = idGenerator.getAndIncrement();
        Order order = new Order(id, request.customerName(), request.status(), Instant.now());
        orders.put(id, order);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Order> update(@PathVariable Long id, @RequestBody OrderRequest request) {
        if (!orders.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        Order existing = orders.get(id);
        Order updated = new Order(id, request.customerName(), request.status(), existing.createdAt());
        orders.put(id, updated);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Order removed = orders.remove(id);
        return removed != null ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    public record Order(Long id, String customerName, String status, Instant createdAt) {
    }

    public record OrderRequest(String customerName, String status) {
    }
}
