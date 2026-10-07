package com.cleanfresh.ms_cleanfresh_bff.service;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Reglas de "quién es quién" que comparten órdenes y notificaciones: el rol
 * sale de las authorities del JWT (grupos de Cognito), la identidad del
 * "username" del access token y la sucursal del Operador de su selector de
 * turno o, si no, del mapeo fijo.
 */
@Component
public class AccesoPorRol {

    // Mapeo mock username de Cognito -> sucursal "de base". No hay base de
    // datos ni claim custom todavía (ver Spec 016); cuando eso exista, este
    // mapa se reemplaza por una consulta real.
    // OJO: "username" en Cognito puede ser el email o un identificador
    // random según cómo esté configurado el User Pool — confirmar el valor
    // real del usuario de prueba Operador y ajustar esta clave si no
    // coincide.
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

    public boolean esAdmin(JwtAuthenticationToken authentication) {
        return tieneRol(authentication, "ROLE_Admin");
    }

    public boolean esOperador(JwtAuthenticationToken authentication) {
        return tieneRol(authentication, "ROLE_Operador");
    }

    /**
     * El access token de Cognito (el que llega acá, no el idToken) no trae
     * "name" ni "preferred_username": el único identificador de la persona es
     * "username".
     */
    public String username(JwtAuthenticationToken authentication) {
        return authentication.getToken().getClaimAsString("username");
    }

    /**
     * Prioridad: sucursal pedida explícitamente (si es válida) -> mapeo fijo
     * username->sucursal -> null (sin sucursal).
     */
    public String resolverSucursal(JwtAuthenticationToken authentication, String sucursalSolicitada) {
        if (sucursalSolicitada != null) {
            for (String valida : SUCURSALES_VALIDAS) {
                if (valida.equalsIgnoreCase(sucursalSolicitada)) {
                    return valida;
                }
            }
        }
        String username = username(authentication);
        if (username == null) {
            return null;
        }
        return SUCURSAL_POR_OPERADOR.get(username.toLowerCase());
    }

    private boolean tieneRol(JwtAuthenticationToken authentication, String rol) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(rol));
    }
}
