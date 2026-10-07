package com.cleanfresh.ms_cleanfresh_bff.service;

import com.cleanfresh.ms_cleanfresh_bff.dto.OrderCreateRequest;
import com.cleanfresh.ms_cleanfresh_bff.dto.OrderRequest;
import com.cleanfresh.ms_cleanfresh_bff.dto.OrderResponse;
import com.cleanfresh.ms_cleanfresh_bff.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final AccesoPorRol acceso;

    public OrderService(OrderRepository orderRepository, AccesoPorRol acceso) {
        this.orderRepository = orderRepository;
        this.acceso = acceso;
    }

    public List<OrderResponse> getAll(JwtAuthenticationToken authentication, String sucursalSolicitada) {
        return filtrarPorSucursalSiCorresponde(orderRepository.findAll(), authentication, sucursalSolicitada);
    }

    public OrderResponse getById(Long id, JwtAuthenticationToken authentication) {
        OrderResponse orden = orderRepository.findById(id);
        if (orden == null) {
            return null;
        }
        if (acceso.esOperador(authentication) && !esDeLaSucursalDelOperador(orden, authentication, null)) {
            return null;
        }
        return orden;
    }

    public List<OrderResponse> getByEstado(
            String estado, JwtAuthenticationToken authentication, String sucursalSolicitada) {
        return filtrarPorSucursalSiCorresponde(
                orderRepository.findByEstado(estado), authentication, sucursalSolicitada);
    }

    private List<OrderResponse> filtrarPorSucursalSiCorresponde(
            List<OrderResponse> ordenes, JwtAuthenticationToken authentication, String sucursalSolicitada) {
        if (!acceso.esOperador(authentication)) {
            return ordenes;
        }
        String sucursal = acceso.resolverSucursal(authentication, sucursalSolicitada);
        if (sucursal == null) {
            return List.of();
        }
        return ordenes.stream()
                .filter(orden -> sucursal.equalsIgnoreCase(orden.sucursal()))
                .toList();
    }

    private boolean esDeLaSucursalDelOperador(
            OrderResponse orden, JwtAuthenticationToken authentication, String sucursalSolicitada) {
        String sucursal = acceso.resolverSucursal(authentication, sucursalSolicitada);
        return sucursal != null && sucursal.equalsIgnoreCase(orden.sucursal());
    }

    // Spec 025: el cliente arma su propio pedido (servicio/precio/sucursal),
    // pero de quién es lo decide el JWT, no el body — así no puede crear
    // un pedido "a nombre de" otra persona.
    public OrderResponse create(JwtAuthenticationToken authentication, OrderRequest request) {
        String cliente = acceso.username(authentication);
        return orderRepository.create(
                new OrderCreateRequest(cliente, request.servicio(), request.total(), request.sucursal())
        );
    }

    /**
     * Spec 030: cambia el estado de una orden. El Admin puede con cualquiera; el
     * Operador solo con las de la sucursal que tiene en turno (403 si es de otra,
     * 404 si no existe). Un estado inválido lo rechaza orders (400).
     */
    public OrderResponse cambiarEstado(
            String numeroOrden, String estado, JwtAuthenticationToken authentication, String sucursalSolicitada) {
        if (acceso.esOperador(authentication) && !acceso.esAdmin(authentication)) {
            OrderResponse orden = orderRepository.findAll().stream()
                    .filter(o -> numeroOrden.equalsIgnoreCase(o.numeroOrden()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada"));
            if (!esDeLaSucursalDelOperador(orden, authentication, sucursalSolicitada)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La orden es de otra sucursal");
            }
        }
        try {
            return orderRepository.updateEstado(numeroOrden, estado);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada");
        } catch (HttpClientErrorException.BadRequest e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido");
        }
    }
}
