# ADR 006 — Backend en capas, organizado por módulo

**Estado:** aceptada · 2026-09-25

## Contexto
Backend Spring Boot escrito por un solo desarrollador en 2 semanas. El dominio es sobre todo CRUD (clientes, catálogo, tratamientos, gastos) más reportes de consulta, sin reglas de negocio complejas ni integraciones externas.

## Decisión
Arquitectura en capas (`controller` → `service` → `repository`), con un paquete por módulo funcional:
`auth`, `catalog`, `client`, `treatment`, `expense`, `report` y `common` (configuración y manejo de errores).
Cada módulo contiene sus propias capas: `controller`, `service`, `repository`, `entity` y `dto`.

- El módulo de servicios del centro se llama `catalog` (con `Category` y `Offering`; la entidad del servicio se llama `Offering` para no chocar con `@Service`) para no confundirlo con la capa `service` de Spring.
- Los controllers solo reciben y responden DTOs; la lógica vive en `service`; el acceso a datos en `repository`.
- Cada controller implementa una interfaz `XxxControllerDocs` con la documentación OpenAPI, para que quede sin anotaciones de Swagger (ADR 012).
- Un módulo puede usar el `service` de otro, pero nunca su `repository` ni sus entidades directamente en un controller.

## Alternativas descartadas
- **Hexagonal (puertos y adaptadores):** mayor desacople, pero exige interfaces, mappers y adaptadores que no aportan valor en un CRUD de este tamaño y plazo.

## Consecuencias
- Menos código y arranque rápido.
- Si el dominio crece en complejidad, se puede extraer la lógica a una capa de dominio módulo por módulo sin reescribir todo.
