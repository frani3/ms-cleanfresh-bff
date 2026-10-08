package com.cleanfresh.ms_cleanfresh_bff.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spec 032: nombre legible del usuario de la sesión. El access token de Cognito solo
 * trae el "username" (un UUID), así que el nombre se le pide a Cognito con el propio
 * token (endpoint /oauth2/userInfo del dominio del Hosted UI): es el nombre real y no
 * lo puede falsificar el navegador. Prefiere el atributo "name", después el "email" y,
 * si Cognito no responde o no trae ninguno, el "username".
 *
 * Solo se guardan en memoria las respuestas exitosas (así un fallo pasajero se
 * reintenta en el pedido siguiente) y por un tiempo acotado.
 */
@Service
public class ClienteNombreResolver {

    private static final Logger log = LoggerFactory.getLogger(ClienteNombreResolver.class);
    private static final Duration VIGENCIA = Duration.ofHours(1);

    private record Entrada(String nombre, Instant vence) {
    }

    private final RestClient cognito;
    private final boolean configurado;
    private final AccesoPorRol acceso;
    private final Map<String, Entrada> cache = new ConcurrentHashMap<>();

    public ClienteNombreResolver(
            @Qualifier("cognitoRestClient") RestClient cognito,
            @Value("${cognito.domain:}") String dominio,
            AccesoPorRol acceso) {
        this.cognito = cognito;
        this.configurado = dominio != null && !dominio.isBlank();
        this.acceso = acceso;
    }

    public String resolver(JwtAuthenticationToken authentication) {
        String username = acceso.username(authentication);
        if (!configurado) {
            return username;
        }
        String clave = authentication.getToken().getSubject();
        Entrada guardada = clave == null ? null : cache.get(clave);
        if (guardada != null && guardada.vence().isAfter(Instant.now())) {
            return guardada.nombre();
        }
        String nombre = consultar(authentication.getToken().getTokenValue());
        if (nombre == null) {
            return username;
        }
        if (clave != null) {
            cache.put(clave, new Entrada(nombre, Instant.now().plus(VIGENCIA)));
        }
        return nombre;
    }

    private String consultar(String accessToken) {
        try {
            Map<String, Object> info = cognito.get()
                    .uri("/oauth2/userInfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            if (info == null) {
                return null;
            }
            String nombre = texto(info.get("name"));
            return nombre != null ? nombre : texto(info.get("email"));
        } catch (RuntimeException e) {
            // La orden se crea igual: se mostrará el identificador, como antes de la Spec 032.
            log.warn("No se pudo obtener el nombre del usuario desde Cognito: {}", e.getMessage());
            return null;
        }
    }

    private static String texto(Object valor) {
        if (valor == null) {
            return null;
        }
        String s = valor.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
