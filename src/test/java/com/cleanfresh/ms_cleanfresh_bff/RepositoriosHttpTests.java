package com.cleanfresh.ms_cleanfresh_bff;

import com.cleanfresh.ms_cleanfresh_bff.dto.NotificacionResponse;
import com.cleanfresh.ms_cleanfresh_bff.dto.OrderResponse;
import com.cleanfresh.ms_cleanfresh_bff.repository.NotificacionRepository;
import com.cleanfresh.ms_cleanfresh_bff.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Los repositorios del BFF contra un servidor HTTP simulado: comprueban la URL
 * que se arma (con tildes y espacios codificados), el cuerpo que se envía y
 * cómo se traducen las respuestas de error de los microservicios.
 */
class RepositoriosHttpTests {

	private static final String AVISOS = """
			[{"id":1,"tipo":"ORDEN_LISTA","destinatarioTipo":"CLIENTE","destinatario":"ana-uuid",
			  "numeroOrden":"ORD-0014","mensaje":"Tu pedido ORD-0014 (Planchado) está listo",
			  "fechaHora":"2026-10-07T12:00:00Z","leida":false}]""";

	private MockRestServiceServer servidor;

	private NotificacionRepository notificaciones() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://notificaciones");
		servidor = MockRestServiceServer.bindTo(builder).build();
		return new NotificacionRepository(builder.build());
	}

	private OrderRepository ordenes() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://orders");
		servidor = MockRestServiceServer.bindTo(builder).build();
		return new OrderRepository(builder.build());
	}

	@Test
	void consultaPorClienteArmaElFiltroCliente() {
		NotificacionRepository repo = notificaciones();
		servidor.expect(requestTo("http://notificaciones/api/notificaciones?cliente=ana-uuid"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(AVISOS, MediaType.APPLICATION_JSON));

		List<NotificacionResponse> avisos = repo.find("ana-uuid", null);

		assertEquals(1, avisos.size());
		assertEquals("ORD-0014", avisos.get(0).numeroOrden());
		servidor.verify();
	}

	@Test
	void consultaPorSucursalCodificaEspaciosYTildes() {
		NotificacionRepository repo = notificaciones();
		servidor.expect(requestTo("http://notificaciones/api/notificaciones?sucursal=Las%20Condes"))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
		servidor.expect(requestTo("http://notificaciones/api/notificaciones?sucursal=%C3%91u%C3%B1oa"))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		repo.find(null, "Las Condes");
		repo.find(null, "Ñuñoa");

		servidor.verify();
	}

	@Test
	void sinFiltroPideTodas() {
		NotificacionRepository repo = notificaciones();
		servidor.expect(requestTo("http://notificaciones/api/notificaciones"))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		repo.find(null, null);

		servidor.verify();
	}

	@Test
	void marcarLeidasUsaPutConElFiltroDelDestinatario() {
		NotificacionRepository repo = notificaciones();
		servidor.expect(requestTo("http://notificaciones/api/notificaciones/leidas?cliente=ana-uuid"))
				.andExpect(method(HttpMethod.PUT))
				.andRespond(withSuccess("{\"marcadas\":1}", MediaType.APPLICATION_JSON));
		servidor.expect(requestTo("http://notificaciones/api/notificaciones/leidas?sucursal=Las%20Condes"))
				.andExpect(method(HttpMethod.PUT))
				.andRespond(withSuccess("{\"marcadas\":2}", MediaType.APPLICATION_JSON));

		repo.marcarLeidas("ana-uuid", null);
		repo.marcarLeidas(null, "Las Condes");

		servidor.verify();
	}

	@Test
	void cambiarEstadoHaceUnPutConElEstadoEnElCuerpo() {
		OrderRepository repo = ordenes();
		servidor.expect(requestTo("http://orders/api/orders/ORD-0014/estado"))
				.andExpect(method(HttpMethod.PUT))
				.andExpect(content().json("{\"estado\":\"DESPACHADO\"}"))
				.andRespond(withSuccess("""
						{"id":14,"numeroOrden":"ORD-0014","cliente":"ana-uuid","servicio":"Planchado",
						 "estado":"DESPACHADO","fecha":"2026-10-07","total":9500.0,"sucursal":"Providencia"}""",
						MediaType.APPLICATION_JSON));

		OrderResponse orden = repo.updateEstado("ORD-0014", "DESPACHADO");

		assertEquals("DESPACHADO", orden.estado());
		servidor.verify();
	}

	@Test
	void losErroresDeOrdersLlegaranComoExcepcionesHttpTipadas() {
		OrderRepository repo = ordenes();
		servidor.expect(requestTo("http://orders/api/orders/ORD-9999/estado"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));
		servidor.expect(requestTo("http://orders/api/orders/ORD-0014/estado"))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST));

		assertThrows(HttpClientErrorException.NotFound.class, () -> repo.updateEstado("ORD-9999", "ACEPTADO"));
		assertThrows(HttpClientErrorException.BadRequest.class, () -> repo.updateEstado("ORD-0014", "VOLANDO"));
	}
}
