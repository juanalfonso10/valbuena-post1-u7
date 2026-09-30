package com.example.multas.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Checkpoints del Paso 12 con la pasarela simulada: 200, 402 por rechazo y 409 por multa ya pagada
@SpringBootTest(properties = "app.pagos.proveedor=simulado")
@AutoConfigureMockMvc
class PagoEnLineaTest {

    @Autowired
    private MockMvc mvc;

    private Integer crearMulta(String estudianteId, int dias) throws Exception {
        String json = mvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"estudianteId\":\"" + estudianteId + "\",\"concepto\":\"Libro\",\"diasAtraso\":" + dias + "}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    @Test
    void pagoAprobadoMarcaLaMultaComoPagada() throws Exception {
        Integer id = crearMulta("EST-PAGO-1", 5); // $2.500
        mvc.perform(post("/api/multas/" + id + "/pagar-en-linea"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("PAGADA"))
            .andExpect(jsonPath("$.metodoPago").value("SIMULADO"));
    }

    @Test
    void pagoRechazadoPorLaPasarelaRetorna402() throws Exception {
        Integer id = crearMulta("EST-PAGO-2", 25); // $12.500, supera el limite de la pasarela simulada
        mvc.perform(post("/api/multas/" + id + "/pagar-en-linea"))
            .andExpect(status().isPaymentRequired())
            .andExpect(jsonPath("$.error").value("Pago rechazado por la pasarela simulada: el monto supera $10.000"));
    }

    @Test
    void pagarEnLineaUnaMultaYaPagadaEnLineaRetorna409() throws Exception {
        Integer id = crearMulta("EST-PAGO-3", 2);
        mvc.perform(post("/api/multas/" + id + "/pagar-en-linea")).andExpect(status().isOk());
        mvc.perform(post("/api/multas/" + id + "/pagar-en-linea")).andExpect(status().isConflict());
    }

    @Test
    void pagarEnLineaUnaMultaPagadaEnVentanillaRetorna409() throws Exception {
        Integer id = crearMulta("EST-PAGO-4", 2);
        mvc.perform(patch("/api/multas/" + id + "/pagar")).andExpect(status().isOk());
        mvc.perform(post("/api/multas/" + id + "/pagar-en-linea")).andExpect(status().isConflict());
    }
}
