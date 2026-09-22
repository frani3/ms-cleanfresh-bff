package com.cleanfresh.ms_cleanfresh_bff.service;

import com.cleanfresh.ms_cleanfresh_bff.dto.OrderCreateRequest;
import com.cleanfresh.ms_cleanfresh_bff.dto.OrderRequest;
import com.cleanfresh.ms_cleanfresh_bff.dto.OrderResponse;
import com.cleanfresh.ms_cleanfresh_bff.repository.OrderRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {

    // Mapeo mock email -> sucursal "de base". No hay base de datos ni claim
    // custom en Azure para esto todavía (ver Spec 016); cuando eso exista,
    // este mapa se reemplaza por una consulta real.
    private static final Map<String, String> SUCURSAL_POR_OPERADOR = Map.of(
            "operador@cleanfreshchain.onmicrosoft.com", "Providencia"
    );

    // Spec 019: el operador puede elegir en qué sucursal está trabajando
    // (como fichar turno), en vez de quedar atado para siempre al mapeo de
    // arriba. Con datos mock esto es aceptable; con operadores reales por
    // sucursal, este selector debería reemplazarse por algo que valide qué
    // sucursales tiene ese usuario realmente asignadas.
    private static final Set<String> SUCURSALES_VALIDAS =
            Set.of("Providencia", "Ñuñoa", "Las Condes", "Maipú");

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<OrderResponse> getAll(JwtAuthenticationToken authentication, String sucursalSolicitada) {
        return filtrarPorSucursalSiCorresponde(orderRepository.findAll(), authentication, sucursalSolicitada);
    }

    public OrderResponse getById(Long id, JwtAuthenticationToken authentication) {
        OrderResponse orden = orderRepository.findById(id);
        if (orden == null) {
            return null;
        }
        if (esOperador(authentication) && !esDeLaSucursalDelOperador(orden, authentication, null)) {
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
        if (!esOperador(authentication)) {
            return ordenes;
        }
        String sucursal = resolverSucursal(authentication, sucursalSolicitada);
        if (sucursal == null) {
            return List.of();
        }
        return ordenes.stream()
                .filter(orden -> sucursal.equalsIgnoreCase(orden.sucursal()))
                .toList();
    }

    private boolean esDeLaSucursalDelOperador(
            OrderResponse orden, JwtAuthenticationToken authentication, String sucursalSolicitada) {
        String sucursal = resolverSucursal(authentication, sucursalSolicitada);
        return sucursal != null && sucursal.equalsIgnoreCase(orden.sucursal());
    }

    private boolean esOperador(JwtAuthenticationToken authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_Operador"));
    }

    // Spec 025: el cliente arma su propio pedido (servicio/precio/sucursal),
    // pero de quién es lo decide el JWT, no el body — así no puede crear
    // un pedido "a nombre de" otra persona.
    public OrderResponse create(JwtAuthenticationToken authentication, OrderRequest request) {
        String cliente = nombreDesdeToken(authentication);
        return orderRepository.create(
                new OrderCreateRequest(cliente, request.servicio(), request.total(), request.sucursal())
        );
    }

    private String nombreDesdeToken(JwtAuthenticationToken authentication) {
        String name = authentication.getToken().getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        return authentication.getToken().getClaimAsString("preferred_username");
    }

    // Prioridad: sucursal pedida explícitamente por query param (si es
    // válida) -> mapeo fijo email->sucursal -> null (sin sucursal).
    private String resolverSucursal(JwtAuthenticationToken authentication, String sucursalSolicitada) {
        if (sucursalSolicitada != null) {
            for (String valida : SUCURSALES_VALIDAS) {
                if (valida.equalsIgnoreCase(sucursalSolicitada)) {
                    return valida;
                }
            }
        }
        String email = authentication.getToken().getClaimAsString("preferred_username");
        if (email == null) {
            return null;
        }
        return SUCURSAL_POR_OPERADOR.get(email.toLowerCase());
    }
}
