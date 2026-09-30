# Post-contenido — Unidad 7: Patrones Arquitectónicos I

## Descripción
Repositorio del post-contenido de la Unidad 7 de Patrones de Diseño de Software. Un único proyecto Spring Boot (`multas-biblioteca-api/`) para la gestión de multas de una biblioteca universitaria. Esta primera parte implementa una API REST en capas (`model/`, `repository/`, `service/`, `controller/`) sobre H2.

## Parte 1 — Arquitectura en capas


| Capa | Paquete | Responsabilidad |
|---|---|---|
| Presentación | `controller/` | `MultaController` expone `/api/multas`; `GlobalExceptionHandler` traduce excepciones de negocio a 400/404/409. |
| Aplicación | `service/` | `MultaService` orquesta los casos de uso y decide las reglas de negocio (tope de multas pendientes). |
| Dominio | `model/` | Entidad `Multa` con su propio comportamiento (`calcularMonto`, `marcarComoPagada`) y excepciones de negocio. |
| Infraestructura | `repository/` | `MultaRepository` (Spring Data JPA) con la consulta agregada `countByEstudianteIdAndEstado`. |

El controlador nunca accede al repositorio: siempre pasa por `MultaService`.

## Estructura de paquetes

```
multas-biblioteca-api/src/main/java/com/example/multas/
├── MultasApplication.java
├── controller/     ← Presentación: MultaController, GenerarMultaRequest, GlobalExceptionHandler
├── service/        ← Aplicación: MultaService
├── model/          ← Dominio: Multa, EstadoMulta y excepciones de negocio
└── repository/     ← Infraestructura: MultaRepository
```

## Cómo ejecutar

```bash
cd multas-biblioteca-api
mvn clean package
mvn spring-boot:run
```

| Método | Ruta | Respuestas |
|---|---|---|
| GET | `/api/multas` | 200 |
| GET | `/api/multas/{id}` | 200 · 404 |
| GET | `/api/multas/estudiante/{estudianteId}` | 200 |
| POST | `/api/multas` | 201 · 400 (validación) · 409 (tope de 3 pendientes) |
| PATCH | `/api/multas/{id}/pagar` | 200 · 409 (ya pagada) |

Los checkpoints están automatizados en `MultaControllerTest` y `MultaServiceTest` (`mvn test`).

## Decisiones de diseño

### Punto de decisión 1 — Cálculo del monto: ¿entidad o Service?
`Multa.calcularMonto` vive en la entidad. El criterio: **si una regla no necesita ningún colaborador externo (Repository, otro Service), es candidata a vivir en el propio objeto de dominio.** El monto depende solo de los días de atraso, que recibe como parámetro. Tenerlo como método privado de `MultaService` también funcionaría, pero dejaría a `Multa` como un contenedor de datos sin comportamiento (modelo anémico). Además, cualquier otro punto que necesitara recalcular un monto tendría que duplicar la fórmula o pasar por el Service sin necesitarlo. Por la misma razón, `marcarComoPagada` también vive en la entidad y protege la invariante "no se paga dos veces".

### Punto de decisión 2 — Conteo de multas pendientes: ¿consulta o filtrado en memoria?
La regla "máximo 3 multas pendientes" sí necesita datos que solo la base de datos conoce. El **dato** se resuelve donde es eficiente, con `countByEstudianteIdAndEstado`, que Spring Data traduce a un `SELECT COUNT(*)`. La **decisión** ("¿se permite generar otra?") la toma exclusivamente `MultaService`. La alternativa, `findByEstudianteId` + filtrar con streams, trae a memoria todo el historial del estudiante en cada creación: con miles de multas por estudiante, el tiempo de respuesta de `POST /api/multas` crecería en proporción al historial, mientras que el `COUNT` devuelve un solo número.

