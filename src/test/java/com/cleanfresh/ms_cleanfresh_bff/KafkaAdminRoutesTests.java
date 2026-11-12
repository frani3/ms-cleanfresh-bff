package com.cleanfresh.ms_cleanfresh_bff;

import com.cleanfresh.ms_cleanfresh_bff.repository.KafkaAdminRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec 044: /api/kafka/** (administrador de Kafka) es solo del Admin. Con jwt() se prueba la autorización por rol; el reenvío se
 * prueba contra un servidor HTTP simulado.
 */
@SpringBootTest(properties = {
		"COGNITO_ISSUER_URI=https://cognito.invalid/pool",
		"COGNITO_CLIENT_ID=test-client"
})
@AutoConfigureMockMvc
class KafkaAdminRoutesTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JwtDecoder jwtDecoder;

	@MockitoBean
	KafkaAdminRepository kafkaAdminRepository;

	private static org.springframework.test.web.servlet.request.RequestPostProcessor rol(String rol) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
	}

	// ---------- quién puede entrar ----------

	@Test
	void sinTokenResponde401() throws Exception {
		mockMvc.perform(get("/api/kafka/topics")).andExpect(status().isUnauthorized());
		verify(kafkaAdminRepository, never()).reenviar(any(), any(), any());
	}

	@Test
	void conRolOperadorResponde403() throws Exception {
		mockMvc.perform(get("/api/kafka/topics").with(rol("Operador"))).andExpect(status().isForbidden());
		verify(kafkaAdminRepository, never()).reenviar(any(), any(), any());
	}

	@Test
	void conRolClienteResponde403EnLecturaYEscritura() throws Exception {
		mockMvc.perform(get("/api/kafka/metricas").with(rol("Cliente"))).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/kafka/topics/x").with(rol("Cliente")).with(csrf())).andExpect(status().isForbidden());
		mockMvc.perform(put("/api/kafka/topics/x/config").with(rol("Cliente")).with(csrf())).andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/kafka/topics/x").with(rol("Cliente"))).andExpect(status().isForbidden());
		verify(kafkaAdminRepository, never()).reenviar(any(), any(), any());
	}

	@Test
	void conRolAdminReenviaYDevuelveLaRespuestaDelAdministrador() throws Exception {
		given(kafkaAdminRepository.reenviar(HttpMethod.GET, "/cluster", null))
				.willReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"clusterId\":\"abc\"}"));

		mockMvc.perform(get("/api/kafka/cluster").with(rol("Admin")))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"clusterId\":\"abc\"}"));
	}

	@Test
	void reenviaPutConCuerpoYDeleteConConsulta() throws Exception {
		given(kafkaAdminRepository.reenviar(HttpMethod.PUT, "/topics/orders.events/config", "{\"configuracion\":{\"retention.ms\":\"1000\"}}"))
				.willReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"accion\":\"configuración actualizada\"}"));
		given(kafkaAdminRepository.reenviar(eq(HttpMethod.DELETE), eq("/topics/pagos?force=true"), isNull()))
				.willReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"accion\":\"tópico eliminado\"}"));

		mockMvc.perform(put("/api/kafka/topics/orders.events/config").with(rol("Admin")).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"configuracion\":{\"retention.ms\":\"1000\"}}"))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"accion\":\"configuración actualizada\"}"));
		mockMvc.perform(delete("/api/kafka/topics/pagos?force=true").with(rol("Admin")))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"accion\":\"tópico eliminado\"}"));
	}

	@Test
	void unCodigoDeErrorDelAdministradorLlegaTalCual() throws Exception {
		given(kafkaAdminRepository.reenviar(eq(HttpMethod.GET), eq("/groups/fantasma"), isNull()))
				.willReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("{\"detail\":\"No existe el consumer group fantasma\"}"));

		mockMvc.perform(get("/api/kafka/groups/fantasma").with(rol("Admin")))
				.andExpect(status().isNotFound())
				.andExpect(content().json("{\"detail\":\"No existe el consumer group fantasma\"}"));
	}

	// ---------- qué rutas se reenvían ----------

	@Test
	void lasRutasDelAdministradorSeReenvian() throws Exception {
		given(kafkaAdminRepository.reenviar(any(), any(), any()))
				.willReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{}"));
		for (String ruta : new String[]{"/api/kafka/cluster", "/api/kafka/metricas", "/api/kafka/topics", "/api/kafka/topics/orders.events",
				"/api/kafka/topics/orders.events/config/retention.ms", "/api/kafka/topics/orders.events/partitions",
				"/api/kafka/groups", "/api/kafka/groups/reportes-kpis", "/api/kafka/groups/reportes-kpis/offsets"}) {
			assertEquals(200, mockMvc.perform(get(ruta).with(rol("Admin"))).andReturn().getResponse().getStatus(), ruta);
		}
		assertEquals(200, mockMvc.perform(post("/api/kafka/topologia/base").with(rol("Admin")).with(csrf())).andReturn().getResponse().getStatus());
	}

	@Test
	void unaRutaQueNoEsDelAdministradorNoSeReenvia() throws Exception {
		for (String ruta : new String[]{"/api/kafka/actuator/health", "/api/kafka/otra", "/api/kafka/topics/a/b/c/d", "/api/kafka/topics/a/otra",
				"/api/kafka/topics/con espacio", "/api/kafka"}) {
			assertEquals(404, mockMvc.perform(get(ruta).with(rol("Admin"))).andReturn().getResponse().getStatus(), ruta);
		}
		verify(kafkaAdminRepository, never()).reenviar(any(), any(), any());
	}

	// ---------- el repositorio ----------

	private KafkaAdminRepository repositorio(MockRestServiceServer[] servidor) {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://kafka-admin");
		servidor[0] = MockRestServiceServer.bindTo(builder).build();
		return new KafkaAdminRepository(builder.build());
	}

	@Test
	void elRepositorioReenviaMetodoRutaYCuerpo() {
		MockRestServiceServer[] servidor = new MockRestServiceServer[1];
		KafkaAdminRepository repo = repositorio(servidor);
		servidor[0].expect(requestTo("http://kafka-admin/api/kafka/topics/pagos/partitions"))
				.andExpect(method(HttpMethod.PUT))
				.andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().json("{\"particiones\":6}"))
				.andRespond(withSuccess("{\"accion\":\"particiones aumentadas\"}", MediaType.APPLICATION_JSON));

		ResponseEntity<String> r = repo.reenviar(HttpMethod.PUT, "/topics/pagos/partitions", "{\"particiones\":6}");

		assertEquals(HttpStatus.OK, r.getStatusCode());
		assertEquals("{\"accion\":\"particiones aumentadas\"}", r.getBody());
		servidor[0].verify();
	}

	@Test
	void elRepositorioDevuelveUn409DelAdministradorSinLanzarExcepcion() {
		MockRestServiceServer[] servidor = new MockRestServiceServer[1];
		KafkaAdminRepository repo = repositorio(servidor);
		servidor[0].expect(requestTo("http://kafka-admin/api/kafka/topics/orders.events"))
				.andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body("{\"detail\":\"El tópico ya existe\"}"));

		ResponseEntity<String> r = repo.reenviar(HttpMethod.POST, "/topics/orders.events", null);

		assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
		assertEquals("{\"detail\":\"El tópico ya existe\"}", r.getBody());
	}

	@Test
	void siElAdministradorNoRespondeSale503() {
		MockRestServiceServer[] servidor = new MockRestServiceServer[1];
		KafkaAdminRepository repo = repositorio(servidor);
		servidor[0].expect(requestTo("http://kafka-admin/api/kafka/metricas"))
				.andRespond(request -> {
					throw new ResourceAccessException("Connection refused");
				});

		ResponseEntity<String> r = repo.reenviar(HttpMethod.GET, "/metricas", null);

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, r.getStatusCode());
	}
}
