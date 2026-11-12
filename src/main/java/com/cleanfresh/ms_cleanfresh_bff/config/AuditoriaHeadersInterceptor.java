package com.cleanfresh.ms_cleanfresh_bff.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

/**
 * Le dice a los microservicios quién hizo cada acción y desde dónde, para el registro de auditoría de Kafka (Spec 044):
 * {@code X-Actor} es el {@code username} del token (lo verificó el BFF; el navegador no puede ponerlo) y {@code X-Origen} la IP
 * del cliente. Se adjuntan a toda llamada saliente que ocurra dentro de una petición con sesión; fuera de una petición (tareas
 * internas) no se agrega nada.
 *
 * <p>La IP sale del <b>último</b> valor de {@code X-Forwarded-For}: es el que agrega el API Gateway (el proxy de confianza); los
 * anteriores los puede inventar quien llama.
 */
public class AuditoriaHeadersInterceptor implements ClientHttpRequestInterceptor {

    public static final String ACTOR = "X-Actor";
    public static final String ORIGEN = "X-Origen";
    private static final int LARGO_MAXIMO = 100;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String actor = actorActual();
        if (actor != null) {
            request.getHeaders().set(ACTOR, actor);
        }
        String origen = origenActual();
        if (origen != null) {
            request.getHeaders().set(ORIGEN, origen);
        }
        return execution.execute(request, body);
    }

    static String actorActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken jwt) {
            return limpiar(jwt.getToken().getClaimAsString("username"));
        }
        return null;
    }

    static String origenActual() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos instanceof ServletRequestAttributes servlet) {
            HttpServletRequest peticion = servlet.getRequest();
            String reenviado = peticion.getHeader("X-Forwarded-For");
            if (reenviado != null && !reenviado.isBlank()) {
                String[] partes = reenviado.split(",");
                return limpiar(partes[partes.length - 1]);
            }
            return limpiar(peticion.getRemoteAddr());
        }
        return null;
    }

    /** Sin saltos de línea (evita inyectar cabeceras) y con un largo razonable. */
    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.replaceAll("[\\r\\n]", "").trim();
        if (limpio.isEmpty()) {
            return null;
        }
        return limpio.length() > LARGO_MAXIMO ? limpio.substring(0, LARGO_MAXIMO) : limpio;
    }
}
