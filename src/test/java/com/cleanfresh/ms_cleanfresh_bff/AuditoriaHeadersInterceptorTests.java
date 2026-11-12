package com.cleanfresh.ms_cleanfresh_bff;

import com.cleanfresh.ms_cleanfresh_bff.config.AuditoriaHeadersInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Spec 044: el BFF le dice a los microservicios quién hizo la acción y desde dónde. */
class AuditoriaHeadersInterceptorTests {

    private final AuditoriaHeadersInterceptor interceptor = new AuditoriaHeadersInterceptor();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private static void sesion(String username) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").claim("username", username).issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private static void peticion(String forwardedFor, String remota) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remota);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private MockClientHttpRequest enviar() throws Exception {
        MockClientHttpRequest salida = new MockClientHttpRequest(HttpMethod.PUT, URI.create("http://orders/api/orders/ORD-1/estado"));
        ClientHttpRequestExecution ejecucion = mock(ClientHttpRequestExecution.class);
        when(ejecucion.execute(any(), any())).thenReturn(new MockClientHttpResponse(new byte[0], HttpStatus.OK));
        interceptor.intercept(salida, new byte[0], ejecucion);
        verify(ejecucion).execute(salida, new byte[0]);
        return salida;
    }

    @Test
    void agregaElUsernameDelTokenYLaIpDelCliente() throws Exception {
        sesion("op-1234");
        peticion("190.5.59.49", "10.0.0.5");

        MockClientHttpRequest salida = enviar();

        assertEquals("op-1234", salida.getHeaders().getFirst("X-Actor"));
        assertEquals("190.5.59.49", salida.getHeaders().getFirst("X-Origen"));
    }

    @Test
    void conVariasIpEnForwardedForUsaLaUltima_laQueAgregaElApiGateway() throws Exception {
        sesion("op-1234");
        peticion("6.6.6.6, 190.5.59.49", "10.0.0.5");

        assertEquals("190.5.59.49", enviar().getHeaders().getFirst("X-Origen"));
    }

    @Test
    void sinForwardedForUsaLaIpDeLaConexion() throws Exception {
        sesion("op-1234");
        peticion(null, "172.31.33.1");

        assertEquals("172.31.33.1", enviar().getHeaders().getFirst("X-Origen"));
    }

    @Test
    void fueraDeUnaPeticionNoAgregaNada() throws Exception {
        MockClientHttpRequest salida = enviar();

        assertNull(salida.getHeaders().getFirst("X-Actor"));
        assertNull(salida.getHeaders().getFirst("X-Origen"));
    }

    @Test
    void sinTokenNoPoneActorPeroSiLaIp() throws Exception {
        peticion("190.5.59.49", "10.0.0.5");

        MockClientHttpRequest salida = enviar();

        assertNull(salida.getHeaders().getFirst("X-Actor"));
        assertEquals("190.5.59.49", salida.getHeaders().getFirst("X-Origen"));
    }

    @Test
    void limpiaLosSaltosDeLineaYAcotaElLargo() throws Exception {
        sesion("op-1\r\nX-Falso: si");
        peticion("a".repeat(300), "10.0.0.5");

        MockClientHttpRequest salida = enviar();

        assertEquals("op-1X-Falso: si", salida.getHeaders().getFirst("X-Actor"));
        assertEquals(100, salida.getHeaders().getFirst("X-Origen").length());
    }
}
