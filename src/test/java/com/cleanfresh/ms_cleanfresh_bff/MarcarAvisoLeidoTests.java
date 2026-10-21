package com.cleanfresh.ms_cleanfresh_bff;

import com.cleanfresh.ms_cleanfresh_bff.repository.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec 034: marcar UN aviso como leído. El destinatario sale siempre del token (no de un parámetro), el
 * Admin es solo lectura y un aviso ajeno o inexistente es 404. Con jwt() se prueba la autorización por rol;
 * el repositorio se prueba contra un servidor HTTP simulado.
 */
@SpringBootTest(properties = {
		"COGNITO_ISSUER_URI=https://cognito.invalid/pool",
		"COGNITO_CLIENT_ID=test-client"
})
@AutoConfigureMockMvc
class MarcarAvisoLeidoTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JwtDecoder jwtDecoder;

	@MockitoBean
	NotificacionRepository notificacionRepository;

	private static RequestPostProcessor como(String rol, String username) {
		return jwt()
				.jwt(j -> j.claim("username", username))
				.authorities(new SimpleGrantedAuthority("ROLE_" + rol));
	}

	// ---------- quién puede y sobre qué avisos ----------

	@Test
	void sinTokenResponde401() throws Exception {
		mockMvc.perform(post("/api/notificaciones/7/leida")).andExpect(status().isUnauthorized());
		verify(notificacionRepository, never()).marcarLeida(any(), any(), any());
	}

	@Test
	void clienteMarcaUnAvisoSuyoUsandoSuUsernameDelToken() throws Exception {
		given(notificacionRepository.marcarLeida(7L, "ana-uuid", null)).willReturn(true);

		mockMvc.perform(post("/api/notificaciones/7/leida").with(como("Cliente", "ana-uuid")))
				.andExpect(status().isNoContent());

		verify(notificacionRepository).marcarLeida(7L, "ana-uuid", null);
	}

	@Test
	void clienteNoPuedeElegirDeQuienEsElAvisoConUnParametro() throws Exception {
		given(notificacionRepository.marcarLeida(7L, "ana-uuid", null)).willReturn(true);

		// aunque mande ?sucursal=..., el filtro sigue saliendo de su token
		mockMvc.perform(post("/api/notificaciones/7/leida").param("sucursal", "Providencia")
						.with(como("Cliente", "ana-uuid")))
				.andExpect(status().isNoContent());

		verify(notificacionRepository).marcarLeida(7L, "ana-uuid", null);
		verify(notificacionRepository, never()).marcarLeida(7L, null, "Providencia");
	}

	@Test
	void unAvisoAjenoOInexistenteResponde404() throws Exception {
		given(notificacionRepository.marcarLeida(9L, "ana-uuid", null)).willReturn(false);

		mockMvc.perform(post("/api/notificaciones/9/leida").with(como("Cliente", "ana-uuid")))
				.andExpect(status().isNotFound());
	}

	@Test
	void operadorMarcaUnAvisoDeSuSucursalEnTurno() throws Exception {
		given(notificacionRepository.marcarLeida(7L, null, "Las Condes")).willReturn(true);

		mockMvc.perform(post("/api/notificaciones/7/leida").param("sucursal", "Las Condes")
						.with(como("Operador", "op-uuid")))
				.andExpect(status().isNoContent());

		verify(notificacionRepository).marcarLeida(7L, null, "Las Condes");
	}

	@Test
	void operadorSinSucursalValidaResponde404SinLlamarAlServicio() throws Exception {
		mockMvc.perform(post("/api/notificaciones/7/leida").with(como("Operador", "op-uuid")))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/notificaciones/7/leida").param("sucursal", "Narnia")
						.with(como("Operador", "op-uuid")))
				.andExpect(status().isNotFound());

		verify(notificacionRepository, never()).marcarLeida(any(), any(), any());
	}

	@Test
	void operadorNoMarcaUnAvisoDeOtraSucursal() throws Exception {
		given(notificacionRepository.marcarLeida(7L, null, "Maipú")).willReturn(false);

		mockMvc.perform(post("/api/notificaciones/7/leida").param("sucursal", "Maipú")
						.with(como("Operador", "op-uuid")))
				.andExpect(status().isNotFound());
	}

	@Test
	void adminEsSoloLecturaYResponde403() throws Exception {
		mockMvc.perform(post("/api/notificaciones/7/leida").with(como("Admin", "admin-uuid")))
				.andExpect(status().isForbidden());

		verify(notificacionRepository, never()).marcarLeida(any(), any(), any());
	}

	@Test
	void unIdQueNoEsNumeroResponde400() throws Exception {
		mockMvc.perform(post("/api/notificaciones/abc/leida").with(como("Cliente", "ana-uuid")))
				.andExpect(status().isBadRequest());

		verify(notificacionRepository, never()).marcarLeida(any(), any(), any());
	}

	// ---------- el repositorio contra un servidor simulado ----------

	private MockRestServiceServer servidor;

	private NotificacionRepository repositorioReal() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://notificaciones");
		servidor = MockRestServiceServer.bindTo(builder).build();
		return new NotificacionRepository(builder.build());
	}

	@Test
	void elRepositorioArmaLaUrlDeUnClienteYDevuelveTrue() {
		NotificacionRepository repo = repositorioReal();
		servidor.expect(requestTo("http://notificaciones/api/notificaciones/7/leida?cliente=ana-uuid"))
				.andExpect(method(HttpMethod.PUT))
				.andRespond(withNoContent());

		assertTrue(repo.marcarLeida(7L, "ana-uuid", null));
		servidor.verify();
	}

	@Test
	void elRepositorioCodificaLasSucursalesConTildesYEspacios() {
		NotificacionRepository repo = repositorioReal();
		servidor.expect(requestTo("http://notificaciones/api/notificaciones/7/leida?sucursal=Las%20Condes"))
				.andRespond(withNoContent());
		servidor.expect(requestTo("http://notificaciones/api/notificaciones/8/leida?sucursal=%C3%91u%C3%B1oa"))
				.andRespond(withNoContent());

		assertTrue(repo.marcarLeida(7L, null, "Las Condes"));
		assertTrue(repo.marcarLeida(8L, null, "Ñuñoa"));
		servidor.verify();
	}

	@Test
	void unNotFoundDelMicroservicioSeTraduceAFalseSinExcepcion() {
		NotificacionRepository repo = repositorioReal();
		servidor.expect(requestTo("http://notificaciones/api/notificaciones/9/leida?cliente=ana-uuid"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertFalse(repo.marcarLeida(9L, "ana-uuid", null));
	}
}
