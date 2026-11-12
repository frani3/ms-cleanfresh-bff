package com.cleanfresh.ms_cleanfresh_bff;

import com.cleanfresh.ms_cleanfresh_bff.repository.AuditoriaRepository;
import com.cleanfresh.ms_cleanfresh_bff.repository.ReportesRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/reportes y /api/auditoria (Spec 044): solo Admin y solo lectura. El post-procesador jwt() entrega la autenticación ya armada
 * con las authorities indicadas, así que prueba la autorización por rol (@PreAuthorize), no la validación del token (eso lo hace el
 * JwtDecoder). El reenvío se prueba contra un servidor HTTP simulado.
 */
@SpringBootTest(properties = {
		"COGNITO_ISSUER_URI=https://cognito.invalid/pool",
		"COGNITO_CLIENT_ID=test-client"
})
@AutoConfigureMockMvc
class AdminRoutesTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JwtDecoder jwtDecoder;

	@MockitoBean
	ReportesRepository reportesRepository;

	@MockitoBean
	AuditoriaRepository auditoriaRepository;

	private static org.springframework.test.web.servlet.request.RequestPostProcessor rol(String rol) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
	}

	private static ResponseEntity<String> json(String cuerpo) {
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(cuerpo);
	}

	// ---------- reportes: quién puede entrar ----------

	@Test
	void reportesSinTokenResponde401() throws Exception {
		mockMvc.perform(get("/api/reportes")).andExpect(status().isUnauthorized());
		verify(reportesRepository, never()).consultar(any());
	}

	@Test
	void reportesConRolOperadorOClienteResponde403() throws Exception {
		mockMvc.perform(get("/api/reportes").with(rol("Operador"))).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/reportes/kpis").with(rol("Cliente"))).andExpect(status().isForbidden());
		verify(reportesRepository, never()).consultar(any());
	}

	@Test
	void reportesConRolAdminReenviaYDevuelveLaRespuestaDelServicio() throws Exception {
		given(reportesRepository.consultar("")).willReturn(json("[{\"branch\":\"Providencia\",\"orders\":42}]"));

		mockMvc.perform(get("/api/reportes").with(rol("Admin")))
				.andExpect(status().isOk())
				.andExpect(content().json("[{\"branch\":\"Providencia\",\"orders\":42}]"));
	}

	@Test
	void reportesReenviaLasRutasNuevasConSuConsulta() throws Exception {
		given(reportesRepository.consultar("/kpis?range=last7d")).willReturn(json("{\"rango\":\"last7d\"}"));
		given(reportesRepository.consultar("/ventas-por-hora")).willReturn(json("[]"));
		given(reportesRepository.consultar("/top-servicios?range=last30d&limite=3")).willReturn(json("[]"));

		mockMvc.perform(get("/api/reportes/kpis?range=last7d").with(rol("Admin")))
				.andExpect(status().isOk()).andExpect(content().json("{\"rango\":\"last7d\"}"));
		mockMvc.perform(get("/api/reportes/ventas-por-hora").with(rol("Admin"))).andExpect(status().isOk());
		mockMvc.perform(get("/api/reportes/top-servicios?range=last30d&limite=3").with(rol("Admin"))).andExpect(status().isOk());
	}

	@Test
	void reportesDevuelveElCodigoDeErrorDelServicioTalCual() throws Exception {
		given(reportesRepository.consultar("/kpis?range=ayer"))
				.willReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("{\"detail\":\"range inválido\"}"));

		mockMvc.perform(get("/api/reportes/kpis?range=ayer").with(rol("Admin")))
				.andExpect(status().isBadRequest())
				.andExpect(content().json("{\"detail\":\"range inválido\"}"));
	}

	@Test
	void reportesNoReenviaRutasQueNoSonDelServicioNiEscrituras() throws Exception {
		for (String ruta : new String[]{"/api/reportes/actuator/health", "/api/reportes/otra", "/api/reportes/kpis/x"}) {
			assertEquals(404, mockMvc.perform(get(ruta).with(rol("Admin"))).andReturn().getResponse().getStatus(), ruta);
		}
		// un intento de salirse de la ruta ni siquiera llega al controlador: lo rechaza el firewall de Spring Security
		assertEquals(400, mockMvc.perform(get("/api/reportes/kpis/../x").with(rol("Admin"))).andReturn().getResponse().getStatus());
		assertEquals(405, mockMvc.perform(post("/api/reportes").with(rol("Admin")).with(csrf())).andReturn().getResponse().getStatus());
		assertEquals(405, mockMvc.perform(delete("/api/reportes").with(rol("Admin"))).andReturn().getResponse().getStatus());
		verify(reportesRepository, never()).consultar(any());
	}

	// ---------- auditoría: quién puede entrar ----------

	@Test
	void auditoriaSinTokenResponde401() throws Exception {
		mockMvc.perform(get("/api/auditoria")).andExpect(status().isUnauthorized());
		verify(auditoriaRepository, never()).consultar(any());
	}

	@Test
	void auditoriaConRolOperadorOClienteResponde403() throws Exception {
		mockMvc.perform(get("/api/auditoria").with(rol("Operador"))).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/auditoria/ordenes/ORD-1").with(rol("Cliente"))).andExpect(status().isForbidden());
		verify(auditoriaRepository, never()).consultar(any());
	}

	@Test
	void auditoriaConRolAdminReenviaLaLineaConSusFiltros() throws Exception {
		given(auditoriaRepository.consultar("?usuario=ana&tipo=OrderCreated&desde=2026-10-15&pagina=1"))
				.willReturn(json("{\"total\":0,\"pagina\":1,\"tamano\":50,\"registros\":[]}"));

		mockMvc.perform(get("/api/auditoria?usuario=ana&tipo=OrderCreated&desde=2026-10-15&pagina=1").with(rol("Admin")))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"total\":0,\"pagina\":1,\"tamano\":50,\"registros\":[]}"));
	}

	@Test
	void auditoriaReenviaLaTrazabilidadYLosTipos() throws Exception {
		given(auditoriaRepository.consultar("/ordenes/ORD-0042")).willReturn(json("[{\"tipo\":\"OrderCreated\"}]"));
		given(auditoriaRepository.consultar("/tipos")).willReturn(json("[\"OrderCreated\"]"));

		mockMvc.perform(get("/api/auditoria/ordenes/ORD-0042").with(rol("Admin"))).andExpect(status().isOk());
		mockMvc.perform(get("/api/auditoria/tipos").with(rol("Admin"))).andExpect(status().isOk());
	}

	@Test
	void auditoriaDevuelveUn404DelServicioTalCual() throws Exception {
		given(auditoriaRepository.consultar("/ordenes/ORD-9999"))
				.willReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("{\"detail\":\"No hay registros\"}"));

		mockMvc.perform(get("/api/auditoria/ordenes/ORD-9999").with(rol("Admin"))).andExpect(status().isNotFound());
	}

	@Test
	void auditoriaNoReenviaRutasAjenasNiEscrituras() throws Exception {
		for (String ruta : new String[]{"/api/auditoria/actuator/health", "/api/auditoria/otra", "/api/auditoria/ordenes", "/api/auditoria/ordenes/a/b"}) {
			assertEquals(404, mockMvc.perform(get(ruta).with(rol("Admin"))).andReturn().getResponse().getStatus(), ruta);
		}
		assertEquals(405, mockMvc.perform(post("/api/auditoria").with(rol("Admin")).with(csrf())).andReturn().getResponse().getStatus());
		assertEquals(405, mockMvc.perform(delete("/api/auditoria/ordenes/ORD-1").with(rol("Admin"))).andReturn().getResponse().getStatus());
		verify(auditoriaRepository, never()).consultar(any());
	}

	// ---------- los repositorios ----------

	private static RestClient cliente(MockRestServiceServer[] servidor) {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://servicio");
		servidor[0] = MockRestServiceServer.bindTo(builder).build();
		return builder.build();
	}

	@Test
	void elRepositorioDeReportesReenviaLaConsultaPorGet() {
		MockRestServiceServer[] servidor = new MockRestServiceServer[1];
		ReportesRepository repo = new ReportesRepository(cliente(servidor));
		servidor[0].expect(requestTo("http://servicio/api/reportes/kpis?range=last24h")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("{\"pedidos\":3}", MediaType.APPLICATION_JSON));

		ResponseEntity<String> r = repo.consultar("/kpis?range=last24h");

		assertEquals(HttpStatus.OK, r.getStatusCode());
		assertEquals("{\"pedidos\":3}", r.getBody());
		servidor[0].verify();
	}

	@Test
	void elRepositorioDeAuditoriaDevuelveUn400DelServicioSinLanzarExcepcion() {
		MockRestServiceServer[] servidor = new MockRestServiceServer[1];
		AuditoriaRepository repo = new AuditoriaRepository(cliente(servidor));
		servidor[0].expect(requestTo("http://servicio/api/auditoria?desde=ayer"))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("{\"detail\":\"fecha inválida\"}"));

		ResponseEntity<String> r = repo.consultar("?desde=ayer");

		assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
		assertEquals("{\"detail\":\"fecha inválida\"}", r.getBody());
	}

	@Test
	void siElServicioNoRespondeSale503() {
		MockRestServiceServer[] servidor = new MockRestServiceServer[1];
		ReportesRepository repo = new ReportesRepository(cliente(servidor));
		servidor[0].expect(requestTo("http://servicio/api/reportes")).andRespond(request -> {
			throw new ResourceAccessException("Connection refused");
		});

		ResponseEntity<String> r = repo.consultar("");

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, r.getStatusCode());
		assertEquals("{\"detail\":\"El servicio de reportes no responde\"}", r.getBody());
	}
}
