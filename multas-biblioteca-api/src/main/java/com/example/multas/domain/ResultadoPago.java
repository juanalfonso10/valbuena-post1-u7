package com.example.multas.domain;

// Tipo de dominio neutral: ninguna pasarela concreta se expone hacia MultaService.
// "referenciaExterna" (y no "idTransaccion" o "reference") para no favorecer el vocabulario de un proveedor.
public record ResultadoPago(
    String proveedor,
    boolean exitoso,
    String referenciaExterna,
    String mensaje
) {}
