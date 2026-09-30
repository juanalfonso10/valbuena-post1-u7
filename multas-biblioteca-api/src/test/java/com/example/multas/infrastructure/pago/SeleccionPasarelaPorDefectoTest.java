package com.example.multas.infrastructure.pago;

import com.example.multas.domain.port.PasarelaPagoPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// Con app.pagos.proveedor=pagosudes (application.properties) el unico bean del puerto es PagosUdesAdapter
@SpringBootTest
class SeleccionPasarelaPorDefectoTest {

    @Autowired
    private PasarelaPagoPort pasarela;

    @Test
    void usaPagosUdes() {
        assertInstanceOf(PagosUdesAdapter.class, pasarela);
    }
}
