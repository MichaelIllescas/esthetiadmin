# ADR 012 — Documentación de la API con OpenAPI (springdoc) e interfaces por controller

**Estado:** aceptada · 2026-09-29

## Contexto
El contrato de la API vive en `docs/api-contract.md`, escrito a mano, y se puede desactualizar. Además, para probar los endpoints se usa una herramienta externa. Se quiere una documentación generada desde el propio código, que se pueda probar con el token, sin ensuciar los controllers con anotaciones.

## Decisión
- **Librería:** `springdoc-openapi-starter-webmvc-ui` 3.x (la serie compatible con Spring Boot 4). Genera el JSON OpenAPI y la interfaz Swagger UI a partir de los controllers.
- **Una interfaz por controller:** cada `XxxController` implementa `XxxControllerDocs` (mismo paquete `controller`). La interfaz lleva `@Tag`, `@Operation`, `@ApiResponse` y `@Parameter`; el controller conserva solo las anotaciones de Spring MVC (`@GetMapping`, `@Valid`, `@RequestBody`, `@PathVariable`, `@RequestParam`) y `@Override`.
- **DTOs:** los modelos se describen con `@Schema` (descripción y ejemplo por campo), sin tocar validaciones.
- **Errores:** cada endpoint documenta sus respuestas 400, 404 y 409 con el formato `ApiError`, según lo que lanzan los servicios.
- **Seguridad:** `OpenApiConfig` declara el esquema Bearer JWT como requisito global (botón "Authorize" en Swagger UI). El login se marca sin requisito (`@SecurityRequirements`).
- **Rutas públicas:** `/swagger-ui/**`, `/swagger-ui.html` y `/v3/api-docs/**` se permiten en `SecurityConfig`.
- **Producción:** `springdoc.api-docs.enabled=false` y `springdoc.swagger-ui.enabled=false` en `application-prod.properties`. En `prod` esas rutas devuelven 404.

## Alternativas descartadas
- **Anotaciones directamente en los controllers:** funciona igual, pero cada método queda con varias líneas de documentación encima de la lógica.
- **Mantener solo `api-contract.md`:** no se puede probar y depende de acordarse de actualizarlo.

## Consecuencias
- Al agregar un endpoint hay que agregarlo también a la interfaz `Docs` del controller (si falta, no se documenta) y usar `@Schema` en sus DTOs nuevos.
- La documentación interactiva está en `http://localhost:8080/swagger-ui.html` (puerto por defecto, perfil `dev`).
- `api-contract.md` sigue siendo la referencia de convenciones (formato, códigos de error); el detalle por endpoint lo da Swagger.
- Nueva dependencia de terceros con versión explícita en el `pom.xml`, que Spring Boot no administra.
