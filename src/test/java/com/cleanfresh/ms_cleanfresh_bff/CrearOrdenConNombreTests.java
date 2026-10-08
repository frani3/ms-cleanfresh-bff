package com.cleanfresh.ms_cleanfresh_bff;

import com.cleanfresh.ms_cleanfresh_bff.dto.OrderCreateRequest;
import com.cleanfresh.ms_cleanfresh_bff.dto.OrderResponse;
import com.cleanfresh.ms_cleanfresh_bff.repository.OrderRepository;
import com.cleanfresh.ms_cleanfresh_bff.service.ClienteNombreResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec 032: al crear una orden, de quién es (cliente) y cómo se llama (clienteNombre) lo
 * deciden el token y Cognito, no el cuerpo de la petición.
 */
@SpringBootTest(properties = {
		"COGNITO_ISSUER_URI=https://cognito.invalid/pool",
		"COGNITO_CLIENT_ID=test-client"
})
@AutoConfigureMockMvc
class CrearOrdenConNombreTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JwtDecoder jwtDecoder;

	@MockitoBean
	OrderRepository orderRepository;

	@MockitoBean
	ClienteNombreResolver nombreResolver;

	@Test
	void laOrdenSeCreaConElIdentificadorDelTokenYElNombreDeCognito() throws Exception {
		given(nombreResolver.resolver(any(JwtAuthenticationToken.class))).willReturn("cliente@cleanfresh.com");
		given(orderRepository.create(any(OrderCreateRequest.class))).willReturn(new OrderResponse(
				7L, "ORD-0007", "ana-uuid", "Planchado", "CREADO", "2026-10-08", 9500.0, "Maipú", "cliente@cleanfresh.com"));

		mockMvc.perform(post("/api/orders")
						.with(jwt().jwt(j -> j.claim("username", "ana-uuid"))
								.authorities(new SimpleGrantedAuthority("ROLE_Cliente")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"servicio\":\"Planchado\",\"total\":9500,\"sucursal\":\"Maipú\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cliente").value("ana-uuid"))
				.andExpect(jsonPath("$.clienteNombre").value("cliente@cleanfresh.com"));

		ArgumentCaptor<OrderCreateRequest> captor = ArgumentCaptor.forClass(OrderCreateRequest.class);
		verify(orderRepository).create(captor.capture());
		assertEquals("ana-uuid", captor.getValue().cliente());
		assertEquals("cliente@cleanfresh.com", captor.getValue().clienteNombre());
		assertEquals("Maipú", captor.getValue().sucursal());
	}

	@Test
	void elClienteNoPuedeColarUnNombreNiUnDuenoPorElCuerpo() throws Exception {
		given(nombreResolver.resolver(any(JwtAuthenticationToken.class))).willReturn("cliente@cleanfresh.com");
		given(orderRepository.create(any(OrderCreateRequest.class))).willReturn(new OrderResponse(
				8L, "ORD-0008", "ana-uuid", "Planchado", "CREADO", "2026-10-08", 9500.0, "Providencia", "cliente@cleanfresh.com"));

		mockMvc.perform(post("/api/orders")
						.with(jwt().jwt(j -> j.claim("username", "ana-uuid"))
								.authorities(new SimpleGrantedAuthority("ROLE_Cliente")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"servicio\":\"Planchado\",\"total\":9500,\"sucursal\":\"Providencia\","
								+ "\"cliente\":\"otra-persona\",\"clienteNombre\":\"Presidente de la empresa\"}"))
				.andExpect(status().isCreated());

		ArgumentCaptor<OrderCreateRequest> captor = ArgumentCaptor.forClass(OrderCreateRequest.class);
		verify(orderRepository).create(captor.capture());
		assertEquals("ana-uuid", captor.getValue().cliente());
		assertEquals("cliente@cleanfresh.com", captor.getValue().clienteNombre());
	}
}
