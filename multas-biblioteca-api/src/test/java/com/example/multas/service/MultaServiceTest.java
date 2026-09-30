package com.example.multas.service;

import com.example.multas.model.EstadoMulta;
import com.example.multas.model.LimiteMultasPendientesException;
import com.example.multas.model.Multa;
import com.example.multas.model.MultaYaPagadaException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MultaServiceTest {

    @Autowired
    private MultaService multaService;

    @Test
    void calculoDelMontoRespetaElTope() {
        assertEquals(new BigDecimal("5000"), Multa.calcularMonto(10));
        assertEquals(new BigDecimal("15000"), Multa.calcularMonto(30));
        assertEquals(new BigDecimal("15000"), Multa.calcularMonto(40));
    }

    @Test
    void noPermiteUnaCuartaMultaPendiente() {
        String estudiante = "EST-SVC-1";
        multaService.generar(estudiante, "Libro Redes", 2);
        multaService.generar(estudiante, "Libro BD", 3);
        multaService.generar(estudiante, "Libro SO", 1);
        assertThrows(LimiteMultasPendientesException.class,
            () -> multaService.generar(estudiante, "Libro Algoritmos", 4));
    }

    @Test
    void pagarEnVentanillaDosVecesLanzaConflicto() {
        Multa multa = multaService.generar("EST-SVC-2", "Calculo II", 5);
        Multa pagada = multaService.pagarEnVentanilla(multa.getId());
        assertEquals(EstadoMulta.PAGADA, pagada.getEstado());
        assertEquals("VENTANILLA", pagada.getMetodoPago());
        assertThrows(MultaYaPagadaException.class, () -> multaService.pagarEnVentanilla(multa.getId()));
    }
}
