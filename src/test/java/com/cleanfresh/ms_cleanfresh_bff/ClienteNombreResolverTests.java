package com.cleanfresh.ms_cleanfresh_bff;

import com.cleanfresh.ms_cleanfresh_bff.service.AccesoPorRol;
import com.cleanfresh.ms_cleanfresh_bff.service.ClienteNombreResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Spec 032: el nombre legible sale de Cognito (/oauth2/userInfo) con el propio access
 * token, y si algo falla se cae al username sin romper la creación de la orden.
 */
class ClienteNombreResolverTests {

	private static final String DOMINIO = "https://cognito.test";
	private static final String USERINFO = DOMINIO + "/oauth2/userInfo";

	private MockRestServiceServer servidor;

	private ClienteNombreResolver resolver(String dominio) {
		RestClient.Builder builder = RestClient.builder();
		if (!dominio.isBlank()) {
			builder.baseUrl(dominio);
		}
		servidor = MockRestServiceServer.bindTo(builder).build();
		return new ClienteNombreResolver(builder.build(), dominio, new AccesoPorRol());
	}

	private static JwtAuthenticationToken sesion(String subject, String username) {
		Jwt jwt = Jwt.withTokenValue("token-de-" + subject)
				.header("alg", "none")
				.subject(subject)
				.claim("username", username)
				.build();
		return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_Cliente")));
	}

	@Test
	void prefiereElNombreYLoPideConElTokenDelUsuario() {
		ClienteNombreResolver resolver = resolver(DOMINIO);
		servidor.expect(requestTo(USERINFO))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Bearer token-de-sub-1"))
				.andRespond(withSuccess("{\"sub\":\"sub-1\",\"name\":\"Cliente Demo\",\"email\":\"cliente@cleanfresh.com\"}",
						MediaType.APPLICATION_JSON));

		assertEquals("Cliente Demo", resolver.resolver(sesion("sub-1", "ana-uuid")));
		servidor.verify();
	}

	@Test
	void sinNombreUsaElCorreo() {
		ClienteNombreResolver resolver = resolver(DOMINIO);
		servidor.expect(requestTo(USERINFO))
				.andRespond(withSuccess("{\"sub\":\"sub-2\",\"email\":\"cliente@cleanfresh.com\"}", MediaType.APPLICATION_JSON));

		assertEquals("cliente@cleanfresh.com", resolver.resolver(sesion("sub-2", "ana-uuid")));
	}

	@Test
	void unNombreEnBlancoSeIgnoraYSeUsaElCorreo() {
		ClienteNombreResolver resolver = resolver(DOMINIO);
		servidor.expect(requestTo(USERINFO))
				.andRespond(withSuccess("{\"name\":\"   \",\"email\":\"cliente@cleanfresh.com\"}", MediaType.APPLICATION_JSON));

		assertEquals("cliente@cleanfresh.com", resolver.resolver(sesion("sub-3", "ana-uuid")));
	}

	@Test
	void siCognitoNoTrajoNingunDatoUsaElUsername() {
		ClienteNombreResolver resolver = resolver(DOMINIO);
		servidor.expect(requestTo(USERINFO))
				.andRespond(withSuccess("{\"sub\":\"sub-4\"}", MediaType.APPLICATION_JSON));

		assertEquals("ana-uuid", resolver.resolver(sesion("sub-4", "ana-uuid")));
	}

	@Test
	void siCognitoFallaUsaElUsernameYNoSeRompe() {
		ClienteNombreResolver resolver = resolver(DOMINIO);
		servidor.expect(requestTo(USERINFO)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
		servidor.expect(requestTo(USERINFO)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		assertEquals("ana-uuid", resolver.resolver(sesion("sub-5", "ana-uuid")));
		assertEquals("ana-uuid", resolver.resolver(sesion("sub-5", "ana-uuid")));
		servidor.verify();
	}

	@Test
	void unFalloPasajeroNoSeGuardaYElPedidoSiguienteLoReintenta() {
		ClienteNombreResolver resolver = resolver(DOMINIO);
		servidor.expect(requestTo(USERINFO)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
		servidor.expect(requestTo(USERINFO))
				.andRespond(withSuccess("{\"email\":\"cliente@cleanfresh.com\"}", MediaType.APPLICATION_JSON));

		assertEquals("ana-uuid", resolver.resolver(sesion("sub-6", "ana-uuid")));
		assertEquals("cliente@cleanfresh.com", resolver.resolver(sesion("sub-6", "ana-uuid")));
		servidor.verify();
	}

	@Test
	void unaRespuestaExitosaSeGuardaYNoSeVuelveAPreguntar() {
		ClienteNombreResolver resolver = resolver(DOMINIO);
		// Una sola respuesta esperada: una segunda llamada a Cognito haría fallar el test.
		servidor.expect(requestTo(USERINFO))
				.andRespond(withSuccess("{\"email\":\"cliente@cleanfresh.com\"}", MediaType.APPLICATION_JSON));

		assertEquals("cliente@cleanfresh.com", resolver.resolver(sesion("sub-7", "ana-uuid")));
		assertEquals("cliente@cleanfresh.com", resolver.resolver(sesion("sub-7", "ana-uuid")));
		servidor.verify();
	}

	@Test
	void cadaUsuarioTieneSuPropioNombre() {
		ClienteNombreResolver resolver = resolver(DOMINIO);
		servidor.expect(requestTo(USERINFO)).andExpect(header("Authorization", "Bearer token-de-sub-8"))
				.andRespond(withSuccess("{\"email\":\"a@cleanfresh.com\"}", MediaType.APPLICATION_JSON));
		servidor.expect(requestTo(USERINFO)).andExpect(header("Authorization", "Bearer token-de-sub-9"))
				.andRespond(withSuccess("{\"email\":\"b@cleanfresh.com\"}", MediaType.APPLICATION_JSON));

		assertEquals("a@cleanfresh.com", resolver.resolver(sesion("sub-8", "u-a")));
		assertEquals("b@cleanfresh.com", resolver.resolver(sesion("sub-9", "u-b")));
		servidor.verify();
	}

	@Test
	void sinDominioConfiguradoUsaElUsernameYNoLlamaANadie() {
		ClienteNombreResolver resolver = resolver("");

		assertEquals("ana-uuid", resolver.resolver(sesion("sub-10", "ana-uuid")));
		servidor.verify();
	}
}
