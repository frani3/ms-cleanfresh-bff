package com.cleanfresh.ms_cleanfresh_bff.service;

import com.cleanfresh.ms_cleanfresh_bff.dto.OrderResponse;
import com.cleanfresh.ms_cleanfresh_bff.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<OrderResponse> getAll() {
        return orderRepository.findAll();
    }

    public OrderResponse getById(Long id) {
        return orderRepository.findById(id);
    }

    public List<OrderResponse> getByEstado(String estado) {
        return orderRepository.findByEstado(estado);
    }
}
