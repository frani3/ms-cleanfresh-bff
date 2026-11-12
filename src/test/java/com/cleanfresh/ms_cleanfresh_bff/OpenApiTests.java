package com.cleanfresh.ms_cleanfresh_bff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec 046: el BFF publica su contrato OpenAPI (Swagger) sin token, porque solo describe los endpoints; llamar a cualquiera de ellos sigue
 * exigiendo el access token de Cognito.
 */
@SpringBootTest(properties = {
		"COGNITO_ISSUER_URI=https://cognito.invalid/pool",
		"COGNITO_CLIENT_ID=test-client"
})
@AutoConfigureMockMvc
class OpenApiTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JwtDecoder jwtDecoder;

	@Test
	void publicaElContratoSinTokenConSusEndpointsYElEsquemaBearer() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("ms-cleanfresh-bff"))
				.andExpect(jsonPath("$.paths['/api/orders']").exists())
				.andExpect(jsonPath("$.paths['/api/catalog']").exists())
				.andExpect(jsonPath("$.paths['/api/notificaciones']").exists())
				.andExpect(jsonPath("$.paths['/api/reportes']").exists())
				.andExpect(jsonPath("$.paths['/api/auditoria']").exists())
				.andExpect(jsonPath("$.components.securitySchemes['cognito-access-token'].scheme").value("bearer"));
	}

	@Test
	void sirveLaInterfazDeSwaggerSinToken() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
	}

	@Test
	void losEndpointsSiguenExigiendoToken() throws Exception {
		mockMvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/reportes/kpis")).andExpect(status().isUnauthorized());
	}
}
