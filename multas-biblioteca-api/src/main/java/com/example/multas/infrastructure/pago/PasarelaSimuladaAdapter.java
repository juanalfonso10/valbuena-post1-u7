package com.example.multas.infrastructure.pago;

import com.example.multas.domain.ResultadoPago;
import com.example.multas.domain.port.PasarelaPagoPort;
import com.example.multas.model.Multa;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Tercer adaptador, para desarrollo y pruebas sin las pasarelas reales: aprueba multas de hasta
// $10.000 y rechaza las mayores. Se agrego sin tocar MultaService ni MultaController.
@Component
@ConditionalOnProperty(prefix = "app.pagos", name = "proveedor", havingValue = "simulado")
public class PasarelaSimuladaAdapter implements PasarelaPagoPort {

    static final BigDecimal MONTO_MAXIMO_APROBADO = new BigDecimal("10000");

    @Override
    public ResultadoPago procesar(Multa multa) {
        boolean exitoso = multa.getMonto().compareTo(MONTO_MAXIMO_APROBADO) <= 0;
        return new ResultadoPago("SIMULADO", exitoso,
            exitoso ? "SIM-" + multa.getId() : null,
            exitoso ? "Pago aprobado por la pasarela simulada"
                    : "Pago rechazado por la pasarela simulada: el monto supera $10.000");
    }
}
