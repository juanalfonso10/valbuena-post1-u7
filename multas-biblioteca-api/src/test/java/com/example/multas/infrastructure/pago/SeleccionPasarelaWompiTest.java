package com.example.multas.infrastructure.pago;

import com.example.multas.domain.port.PasarelaPagoPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// Solo cambiando la propiedad (sin recompilar Controller ni Service) el puerto pasa a ser WompiAdapter
@SpringBootTest(properties = "app.pagos.proveedor=wompi")
class SeleccionPasarelaWompiTest {

    @Autowired
    private PasarelaPagoPort pasarela;

    @Test
    void usaWompi() {
        assertInstanceOf(WompiAdapter.class, pasarela);
    }
}
