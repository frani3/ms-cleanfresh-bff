package com.cleanfresh.ms_cleanfresh_bff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"COGNITO_ISSUER_URI=https://cognito.invalid/pool",
		"COGNITO_CLIENT_ID=test-client"
})
class MsCleanfreshBffApplicationTests {

	// El decoder real consulta al emisor (Cognito) al crearse; en los tests se
	// reemplaza para que no dependan de la red ni de un User Pool existente.
	@MockitoBean
	JwtDecoder jwtDecoder;

	@Test
	void contextLoads() {
	}

}
