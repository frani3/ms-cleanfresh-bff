package com.cleanfresh.ms_cleanfresh_bff.service;

import com.cleanfresh.ms_cleanfresh_bff.dto.NotificacionResponse;
import com.cleanfresh.ms_cleanfresh_bff.repository.NotificacionRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spec 030: decide qué avisos ve cada rol. El filtro sale siempre del JWT
 * (nunca de un parámetro que mande el cliente, salvo la sucursal en turno del
 * Operador, que se valida):
 * <ul>
 *   <li>Admin: todos (solo lectura: no los marca como leídos, para no borrarles
 *       los pendientes a los Operadores y Clientes).</li>
 *   <li>Operador: los de la sucursal que tiene en turno.</li>
 *   <li>Cliente: solo los suyos, por el username del access token.</li>
 * </ul>
 */
@Service
public class NotificacionService {

    private final NotificacionRepository repository;
    private final AccesoPorRol acceso;

    public NotificacionService(NotificacionRepository repository, AccesoPorRol acceso) {
        this.repository = repository;
        this.acceso = acceso;
    }

    public List<NotificacionResponse> listar(JwtAuthenticationToken authentication, String sucursalSolicitada) {
        if (acceso.esAdmin(authentication)) {
            return repository.find(null, null);
        }
        if (acceso.esOperador(authentication)) {
            String sucursal = acceso.resolverSucursal(authentication, sucursalSolicitada);
            return sucursal == null ? List.of() : repository.find(null, sucursal);
        }
        String cliente = acceso.username(authentication);
        return cliente == null || cliente.isBlank() ? List.of() : repository.find(cliente, null);
    }

    public void marcarLeidas(JwtAuthenticationToken authentication, String sucursalSolicitada) {
        if (acceso.esAdmin(authentication)) {
            return;
        }
        if (acceso.esOperador(authentication)) {
            String sucursal = acceso.resolverSucursal(authentication, sucursalSolicitada);
            if (sucursal != null) {
                repository.marcarLeidas(null, sucursal);
            }
            return;
        }
        String cliente = acceso.username(authentication);
        if (cliente != null && !cliente.isBlank()) {
            repository.marcarLeidas(cliente, null);
        }
    }
}
