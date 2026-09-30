package com.example.multas.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Checkpoints del Paso 6: 201, 400, 409 por tope, 404 y 409 por doble pago
@SpringBootTest
@AutoConfigureMockMvc
class MultaControllerTest {

    @Autowired
    private MockMvc mvc;

    private String multa(String estudianteId, int dias) {
        return "{\"estudianteId\":\"" + estudianteId + "\",\"concepto\":\"Libro\",\"diasAtraso\":" + dias + "}";
    }

    private Integer crearMulta(String estudianteId, int dias) throws Exception {
        String json = mvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON)
                .content(multa(estudianteId, dias)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    @Test
    void generarRetorna201ConElMontoCalculado() throws Exception {
        mvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON).content(multa("EST-API-1", 4)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.monto").value(2000))
            .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void generarSinEstudianteRetorna400() throws Exception {
        mvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"concepto\":\"Libro\",\"diasAtraso\":2}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.estudianteId").exists());
    }

    @Test
    void cuartaMultaPendienteRetorna409() throws Exception {
        crearMulta("EST-API-2", 1);
        crearMulta("EST-API-2", 2);
        crearMulta("EST-API-2", 3);
        mvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON).content(multa("EST-API-2", 4)))
            .andExpect(status().isConflict());
    }

    @Test
    void multaInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/multas/999999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void pagarEnVentanillaYRepetirRetorna409() throws Exception {
        Integer id = crearMulta("EST-API-3", 2);
        mvc.perform(patch("/api/multas/" + id + "/pagar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("PAGADA"))
            .andExpect(jsonPath("$.metodoPago").value("VENTANILLA"));
        mvc.perform(patch("/api/multas/" + id + "/pagar"))
            .andExpect(status().isConflict());
    }
}
