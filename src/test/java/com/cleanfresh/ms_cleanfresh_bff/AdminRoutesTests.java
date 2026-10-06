package com.cleanfresh.ms_cleanfresh_bff;

import com.cleanfresh.ms_cleanfresh_bff.dto.AuditEntryResponse;
import com.cleanfresh.ms_cleanfresh_bff.dto.BranchReportResponse;
import com.cleanfresh.ms_cleanfresh_bff.repository.AuditoriaRepository;
import com.cleanfresh.ms_cleanfresh_bff.repository.ReportesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rutas nuevas de EP2 (/api/reportes y /api/auditoria): solo Admin.
 * El post-procesador jwt() entrega la autenticación ya armada con las
 * authorities indicadas, así que prueba la autorización por rol
 * (@PreAuthorize), no la validación del token (eso lo hace el JwtDecoder).
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

	@Test
	void reportesSinTokenResponde401() throws Exception {
		mockMvc.perform(get("/api/reportes")).andExpect(status().isUnauthorized());
	}

	@Test
	void reportesConRolOperadorResponde403() throws Exception {
		mockMvc.perform(get("/api/reportes").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Operador"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void reportesConRolClienteResponde403() throws Exception {
		mockMvc.perform(get("/api/reportes").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Cliente"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void reportesConRolAdminResponde200ConLosDatosDelServicio() throws Exception {
		given(reportesRepository.findAll()).willReturn(List.of(new BranchReportResponse("Providencia", 42)));

		mockMvc.perform(get("/api/reportes").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].branch").value("Providencia"))
				.andExpect(jsonPath("$[0].orders").value(42));
	}

	@Test
	void auditoriaSinTokenResponde401() throws Exception {
		mockMvc.perform(get("/api/auditoria")).andExpect(status().isUnauthorized());
	}

	@Test
	void auditoriaConRolOperadorResponde403() throws Exception {
		mockMvc.perform(get("/api/auditoria").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Operador"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void auditoriaConRolAdminResponde200ConLosDatosDelServicio() throws Exception {
		given(auditoriaRepository.findAll())
				.willReturn(List.of(new AuditEntryResponse("11:32", "operador@cf.co", "Accion", "info")));

		mockMvc.perform(get("/api/auditoria").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].actor").value("operador@cf.co"))
				.andExpect(jsonPath("$[0].level").value("info"));
	}
}
