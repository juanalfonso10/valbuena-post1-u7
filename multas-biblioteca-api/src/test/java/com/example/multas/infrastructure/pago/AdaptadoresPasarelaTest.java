package com.example.multas.infrastructure.pago;

import com.example.multas.domain.ResultadoPago;
import com.example.multas.model.Multa;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Cada adaptador traduce un contrato HTTP distinto al mismo ResultadoPago (Punto de decision 4)
class AdaptadoresPasarelaTest {

    private static final String URL_UDES = "http://pasarela.test/pagosudes";
    private static final String URL_WOMPI = "http://pasarela.test/wompi";

    private Multa multaDe(String monto) {
        Multa multa = new Multa();
        multa.setId(7L);
        multa.setEstudianteId("EST-7");
        multa.setMonto(new BigDecimal(monto));
        return multa;
    }

    @Test
    void pagosUdesTraduceIdTransaccionYEstadoAprobada() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(restTemplate).build();
        servidor.expect(requestTo(URL_UDES)).andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.estudianteId").value("EST-7"))
            .andRespond(withSuccess("{\"idTransaccion\":\"TX-99\",\"estadoTransaccion\":\"APROBADA\"}",
                MediaType.APPLICATION_JSON));

        ResultadoPago resultado = new PagosUdesAdapter(restTemplate, URL_UDES).procesar(multaDe("2500"));

        assertTrue(resultado.exitoso());
        assertEquals("PAGOSUDES", resultado.proveedor());
        assertEquals("TX-99", resultado.referenciaExterna());
        servidor.verify();
    }

    @Test
    void wompiEnviaCentavosYTraduceReferenceYStatus() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(restTemplate).build();
        servidor.expect(requestTo(URL_WOMPI)).andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.amountInCents").value(250000))
            .andExpect(jsonPath("$.reference").value("multa-7"))
            .andRespond(withSuccess("{\"reference\":\"multa-7\",\"status\":\"APPROVED\"}",
                MediaType.APPLICATION_JSON));

        ResultadoPago resultado = new WompiAdapter(restTemplate, URL_WOMPI).procesar(multaDe("2500"));

        assertTrue(resultado.exitoso());
        assertEquals("WOMPI", resultado.proveedor());
        assertEquals("multa-7", resultado.referenciaExterna());
        servidor.verify();
    }

    @Test
    void pasarelaCaidaSeTraduceEnPagoNoExitoso() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(restTemplate).build();
        servidor.expect(requestTo(URL_UDES)).andRespond(withServerError());

        ResultadoPago resultado = new PagosUdesAdapter(restTemplate, URL_UDES).procesar(multaDe("2500"));

        assertFalse(resultado.exitoso());
        assertTrue(resultado.mensaje().startsWith("PagosUDES no disponible"));
    }

    @Test
    void pasarelaSimuladaRechazaMontosMayoresADiezMil() {
        PasarelaSimuladaAdapter simulada = new PasarelaSimuladaAdapter();
        assertTrue(simulada.procesar(multaDe("10000")).exitoso());
        assertFalse(simulada.procesar(multaDe("12500")).exitoso());
    }
}
