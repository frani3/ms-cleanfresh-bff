package com.cleanfresh.ms_cleanfresh_bff;

import com.cleanfresh.ms_cleanfresh_bff.repository.MqAdminRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec 033: /api/mq/** (administrador de RabbitMQ) es solo del Admin. Con jwt() se prueba la autorización
 * por rol; el reenvío se prueba contra un servidor HTTP simulado.
 */
@SpringBootTest(properties = {
		"COGNITO_ISSUER_URI=https://cognito.invalid/pool",
		"COGNITO_CLIENT_ID=test-client"
})
@AutoConfigureMockMvc
class MqAdminRoutesTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JwtDecoder jwtDecoder;

	@MockitoBean
	MqAdminRepository mqAdminRepository;

	private static org.springframework.test.web.servlet.request.RequestPostProcessor rol(String rol) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
	}

	// ---------- quién puede entrar ----------

	@Test
	void sinTokenResponde401() throws Exception {
		mockMvc.perform(get("/api/mq/topologia")).andExpect(status().isUnauthorized());
		verify(mqAdminRepository, never()).reenviar(any(), any(), any());
	}

	@Test
	void conRolOperadorResponde403() throws Exception {
		mockMvc.perform(get("/api/mq/topologia").with(rol("Operador"))).andExpect(status().isForbidden());
		verify(mqAdminRepository, never()).reenviar(any(), any(), any());
	}

	@Test
	void conRolClienteResponde403EnLecturaYEscritura() throws Exception {
		mockMvc.perform(get("/api/mq/metricas").with(rol("Cliente"))).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/mq/queues/q.x").with(rol("Cliente")).with(csrf()))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/mq/queues/q.x").with(rol("Cliente"))).andExpect(status().isForbidden());
		verify(mqAdminRepository, never()).reenviar(any(), any(), any());
	}

	@Test
	void conRolAdminReenviaYDevuelveLaRespuestaDelAdministrador() throws Exception {
		given(mqAdminRepository.reenviar(HttpMethod.GET, "/topologia", null))
				.willReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"nodos\":[\"rabbit@rabbit1 (activo)\"]}"));

		mockMvc.perform(get("/api/mq/topologia").with(rol("Admin")))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"nodos\":[\"rabbit@rabbit1 (activo)\"]}"));
	}

	@Test
	void reenviaLaConsultaYElCuerpoDeUnDelete() throws Exception {
		given(mqAdminRepository.reenviar(eq(HttpMethod.DELETE), eq("/queues/q.demo?conDlq=true"), isNull()))
				.willReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"accion\":\"cola eliminada\"}"));
		given(mqAdminRepository.reenviar(HttpMethod.POST, "/bindings", "{\"exchange\":\"cmd.topic\",\"cola\":\"q\",\"clave\":\"k\"}"))
				.willReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"accion\":\"binding creado\"}"));

		mockMvc.perform(delete("/api/mq/queues/q.demo?conDlq=true").with(rol("Admin")))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"accion\":\"cola eliminada\"}"));
		mockMvc.perform(post("/api/mq/bindings").with(rol("Admin"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"exchange\":\"cmd.topic\",\"cola\":\"q\",\"clave\":\"k\"}"))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"accion\":\"binding creado\"}"));
	}

	@Test
	void unCodigoDeErrorDelAdministradorLlegaTalCual() throws Exception {
		given(mqAdminRepository.reenviar(eq(HttpMethod.DELETE), eq("/queues/q.fantasma"), isNull()))
				.willReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("{\"detail\":\"No existe en RabbitMQ\"}"));

		mockMvc.perform(delete("/api/mq/queues/q.fantasma").with(rol("Admin")))
				.andExpect(status().isNotFound())
				.andExpect(content().json("{\"detail\":\"No existe en RabbitMQ\"}"));
	}

	// ---------- qué rutas se reenvían ----------

	@Test
	void unaRutaQueNoEsDelAdministradorNoSeReenvia() throws Exception {
		for (String ruta : new String[]{"/api/mq/actuator/health", "/api/mq/otra", "/api/mq/queues", "/api/mq/queues/a/b", "/api/mq"}) {
			assertEquals(404, mockMvc.perform(get(ruta).with(rol("Admin"))).andReturn().getResponse().getStatus(), ruta);
		}
		verify(mqAdminRepository, never()).reenviar(any(), any(), any());
	}

	// ---------- el repositorio ----------

	private MqAdminRepository repositorio(MockRestServiceServer[] servidor) {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://mq-admin");
		servidor[0] = MockRestServiceServer.bindTo(builder).build();
		return new MqAdminRepository(builder.build());
	}

	@Test
	void elRepositorioReenviaMetodoRutaYCuerpo() {
		MockRestServiceServer[] servidor = new MockRestServiceServer[1];
		MqAdminRepository repo = repositorio(servidor);
		servidor[0].expect(requestTo("http://mq-admin/api/mq/bindings"))
				.andExpect(method(HttpMethod.DELETE))
				.andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().json("{\"clave\":\"email.*\"}"))
				.andRespond(withSuccess("{\"accion\":\"binding eliminado\"}", MediaType.APPLICATION_JSON));

		ResponseEntity<String> r = repo.reenviar(HttpMethod.DELETE, "/bindings", "{\"clave\":\"email.*\"}");

		assertEquals(HttpStatus.OK, r.getStatusCode());
		assertEquals("{\"accion\":\"binding eliminado\"}", r.getBody());
		servidor[0].verify();
	}

	@Test
	void elRepositorioDevuelveUn404DelAdministradorSinLanzarExcepcion() {
		MockRestServiceServer[] servidor = new MockRestServiceServer[1];
		MqAdminRepository repo = repositorio(servidor);
		servidor[0].expect(requestTo("http://mq-admin/api/mq/queues/q.fantasma"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("{\"detail\":\"No existe en RabbitMQ\"}"));

		ResponseEntity<String> r = repo.reenviar(HttpMethod.DELETE, "/queues/q.fantasma", null);

		assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
		assertEquals("{\"detail\":\"No existe en RabbitMQ\"}", r.getBody());
	}

	@Test
	void siElAdministradorNoRespondeSale503() {
		MockRestServiceServer[] servidor = new MockRestServiceServer[1];
		MqAdminRepository repo = repositorio(servidor);
		servidor[0].expect(requestTo("http://mq-admin/api/mq/metricas"))
				.andRespond(request -> {
					throw new ResourceAccessException("Connection refused");
				});

		ResponseEntity<String> r = repo.reenviar(HttpMethod.GET, "/metricas", null);

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, r.getStatusCode());
	}
}
