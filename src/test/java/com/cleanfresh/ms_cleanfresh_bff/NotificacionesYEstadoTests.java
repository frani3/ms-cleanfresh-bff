package com.cleanfresh.ms_cleanfresh_bff;

import com.cleanfresh.ms_cleanfresh_bff.dto.NotificacionResponse;
import com.cleanfresh.ms_cleanfresh_bff.dto.OrderResponse;
import com.cleanfresh.ms_cleanfresh_bff.repository.NotificacionRepository;
import com.cleanfresh.ms_cleanfresh_bff.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec 030: cambio de estado de una orden y avisos filtrados por rol. El
 * post-procesador jwt() entrega la autenticación ya armada, así que prueba la
 * autorización y el filtrado (no la validación del token, que hace el
 * JwtDecoder).
 */
@SpringBootTest(properties = {
		"COGNITO_ISSUER_URI=https://cognito.invalid/pool",
		"COGNITO_CLIENT_ID=test-client"
})
@AutoConfigureMockMvc
class NotificacionesYEstadoTests {

	private static final String JSON_ESTADO = "{\"estado\":\"DESPACHADO\"}";

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JwtDecoder jwtDecoder;

	@MockitoBean
	OrderRepository orderRepository;

	@MockitoBean
	NotificacionRepository notificacionRepository;

	private static RequestPostProcessor como(String rol, String username) {
		return jwt()
				.jwt(j -> j.claim("username", username))
				.authorities(new SimpleGrantedAuthority("ROLE_" + rol));
	}

	private static OrderResponse orden(String numero, String sucursal, String estado) {
		return new OrderResponse(1L, numero, "ana-uuid", "Planchado", estado, "2026-10-07", 9500.0, sucursal);
	}

	@BeforeEach
	void ordenes() {
		given(orderRepository.findAll()).willReturn(List.of(
				orden("ORD-0014", "Providencia", "CREADO"),
				orden("ORD-0015", "Las Condes", "CREADO")));
	}

	// ---------- PUT /api/orders/{numeroOrden}/estado ----------

	@Test
	void cambiarEstadoSinTokenResponde401() throws Exception {
		mockMvc.perform(put("/api/orders/ORD-0014/estado").contentType(MediaType.APPLICATION_JSON).content(JSON_ESTADO))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void cambiarEstadoConRolClienteResponde403() throws Exception {
		mockMvc.perform(put("/api/orders/ORD-0014/estado").with(como("Cliente", "ana-uuid"))
						.contentType(MediaType.APPLICATION_JSON).content(JSON_ESTADO))
				.andExpect(status().isForbidden());
		verify(orderRepository, never()).updateEstado(anyString(), anyString());
	}

	@Test
	void operadorCambiaUnaOrdenDeLaSucursalQueTieneEnTurno() throws Exception {
		given(orderRepository.updateEstado("ORD-0014", "DESPACHADO"))
				.willReturn(orden("ORD-0014", "Providencia", "DESPACHADO"));

		mockMvc.perform(put("/api/orders/ORD-0014/estado").param("sucursal", "Providencia")
						.with(como("Operador", "op-uuid"))
						.contentType(MediaType.APPLICATION_JSON).content(JSON_ESTADO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("DESPACHADO"));
	}

	@Test
	void operadorNoPuedeCambiarUnaOrdenDeOtraSucursal() throws Exception {
		mockMvc.perform(put("/api/orders/ORD-0015/estado").param("sucursal", "Providencia")
						.with(como("Operador", "op-uuid"))
						.contentType(MediaType.APPLICATION_JSON).content(JSON_ESTADO))
				.andExpect(status().isForbidden());
		verify(orderRepository, never()).updateEstado(anyString(), anyString());
	}

	@Test
	void operadorSinSucursalEnTurnoNoPuedeCambiarNada() throws Exception {
		mockMvc.perform(put("/api/orders/ORD-0014/estado")
						.with(como("Operador", "op-uuid"))
						.contentType(MediaType.APPLICATION_JSON).content(JSON_ESTADO))
				.andExpect(status().isForbidden());
	}

	@Test
	void operadorConUnaOrdenInexistenteRecibe404() throws Exception {
		mockMvc.perform(put("/api/orders/ORD-9999/estado").param("sucursal", "Providencia")
						.with(como("Operador", "op-uuid"))
						.contentType(MediaType.APPLICATION_JSON).content(JSON_ESTADO))
				.andExpect(status().isNotFound());
	}

	@Test
	void adminCambiaCualquierOrden() throws Exception {
		given(orderRepository.updateEstado("ORD-0015", "DESPACHADO"))
				.willReturn(orden("ORD-0015", "Las Condes", "DESPACHADO"));

		mockMvc.perform(put("/api/orders/ORD-0015/estado").with(como("Admin", "admin-uuid"))
						.contentType(MediaType.APPLICATION_JSON).content(JSON_ESTADO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.numeroOrden").value("ORD-0015"));
	}

	@Test
	void unEstadoInvalidoQueRechazaOrdersSeTraduceA400() throws Exception {
		given(orderRepository.updateEstado(anyString(), eq("VOLANDO"))).willThrow(
				HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "x", HttpHeaders.EMPTY, new byte[0], null));

		mockMvc.perform(put("/api/orders/ORD-0014/estado").with(como("Admin", "admin-uuid"))
						.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"VOLANDO\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unaOrdenQueOrdersNoConoceSeTraduceA404() throws Exception {
		given(orderRepository.updateEstado(eq("ORD-9999"), anyString())).willThrow(
				HttpClientErrorException.create(HttpStatus.NOT_FOUND, "x", HttpHeaders.EMPTY, new byte[0], null));

		mockMvc.perform(put("/api/orders/ORD-9999/estado").with(como("Admin", "admin-uuid"))
						.contentType(MediaType.APPLICATION_JSON).content(JSON_ESTADO))
				.andExpect(status().isNotFound());
	}

	// ---------- GET /api/notificaciones ----------

	private static NotificacionResponse aviso(String destinatarioTipo, String destinatario, String numero) {
		return new NotificacionResponse(1L, "ORDEN_LISTA", destinatarioTipo, destinatario, numero,
				"mensaje", "2026-10-07T12:00:00Z", false);
	}

	@Test
	void notificacionesSinTokenResponde401() throws Exception {
		mockMvc.perform(get("/api/notificaciones")).andExpect(status().isUnauthorized());
	}

	@Test
	void clienteSoloVeSusAvisosPorElUsernameDelToken() throws Exception {
		given(notificacionRepository.find("ana-uuid", null))
				.willReturn(List.of(aviso("CLIENTE", "ana-uuid", "ORD-0014")));

		mockMvc.perform(get("/api/notificaciones").with(como("Cliente", "ana-uuid")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].destinatario").value("ana-uuid"));
	}

	@Test
	void clienteNoPuedeVerLosAvisosDeOtroMandandoParametros() throws Exception {
		given(notificacionRepository.find("ana-uuid", null)).willReturn(List.of());

		mockMvc.perform(get("/api/notificaciones").param("cliente", "luis-uuid").param("sucursal", "Providencia")
						.with(como("Cliente", "ana-uuid")))
				.andExpect(status().isOk());

		verify(notificacionRepository).find("ana-uuid", null);
		verify(notificacionRepository, never()).find(eq("luis-uuid"), any());
		verify(notificacionRepository, never()).find(any(), eq("Providencia"));
	}

	@Test
	void operadorVeLosAvisosDeLaSucursalQueTieneEnTurno() throws Exception {
		given(notificacionRepository.find(null, "Providencia"))
				.willReturn(List.of(aviso("SUCURSAL", "Providencia", "ORD-0014")));

		mockMvc.perform(get("/api/notificaciones").param("sucursal", "Providencia")
						.with(como("Operador", "op-uuid")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].destinatario").value("Providencia"));
	}

	@Test
	void operadorSinSucursalValidaNoVeNadaYNoSeConsultaElServicio() throws Exception {
		mockMvc.perform(get("/api/notificaciones").param("sucursal", "Narnia")
						.with(como("Operador", "op-uuid")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		verify(notificacionRepository, never()).find(any(), any());
	}

	@Test
	void adminVeTodosLosAvisos() throws Exception {
		given(notificacionRepository.find(null, null)).willReturn(List.of(
				aviso("SUCURSAL", "Providencia", "ORD-0014"), aviso("CLIENTE", "ana-uuid", "ORD-0014")));

		mockMvc.perform(get("/api/notificaciones").with(como("Admin", "admin-uuid")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	// ---------- POST /api/notificaciones/leidas ----------

	@Test
	void marcarLeidasSinTokenResponde401() throws Exception {
		mockMvc.perform(post("/api/notificaciones/leidas")).andExpect(status().isUnauthorized());
	}

	@Test
	void clienteMarcaComoLeidasSoloLasSuyas() throws Exception {
		mockMvc.perform(post("/api/notificaciones/leidas").with(como("Cliente", "ana-uuid")))
				.andExpect(status().isNoContent());

		verify(notificacionRepository).marcarLeidas("ana-uuid", null);
	}

	@Test
	void operadorMarcaComoLeidasLasDeSuSucursal() throws Exception {
		mockMvc.perform(post("/api/notificaciones/leidas").param("sucursal", "Las Condes")
						.with(como("Operador", "op-uuid")))
				.andExpect(status().isNoContent());

		verify(notificacionRepository).marcarLeidas(null, "Las Condes");
	}

	@Test
	void adminNoMarcaNadaComoLeido() throws Exception {
		mockMvc.perform(post("/api/notificaciones/leidas").with(como("Admin", "admin-uuid")))
				.andExpect(status().isNoContent());

		verify(notificacionRepository, never()).marcarLeidas(any(), any());
	}
}
