# EsthetiAdmin — gestión para centros de estética — Stack y restricciones

| Capa | Tecnología | Dónde corre |
|---|---|---|
| Frontend | Angular | hosting estático (servidor web) |
| Backend | Spring Boot | VPS propio, en Docker |
| Base de datos | MySQL | VPS propio, instancia compartida (ADR 011) |
| Documentación de la API | OpenAPI con springdoc (Swagger UI) | Solo en `dev` (ADR 012) |

## Restricciones
- Un solo desarrollador, 2 semanas.
- Frontend y backend en dominios distintos: requiere HTTPS y CORS configurado (ADR 002).
- Uso interno de una sola usuaria; datos personales de clientas, por eso hay login (ADR 003).

## Decisiones
Ver [adr/](adr/):
- [001 Stack](adr/001-stack.md)
- [002 Despliegue separado](adr/002-despliegue-separado.md)
- [003 Usuario único](adr/003-usuario-unico.md)
- [004 Importe del tratamiento](adr/004-importe-tratamiento.md)
- [005 Desactivar en vez de borrar](adr/005-desactivar-en-vez-de-borrar.md)
- [006 Backend en capas por módulo](adr/006-arquitectura-en-capas.md)
- [007 Frontend por funcionalidad](adr/007-estructura-frontend.md)
- [008 Autenticación con JWT](adr/008-autenticacion-jwt.md)
- [009 Reportes como modelo de lectura](adr/009-reportes-como-modelo-de-lectura.md)
- [010 Gráficos con Chart.js](adr/010-graficos-con-chartjs.md)
- [011 Despliegue en producción](adr/011-despliegue-produccion.md)
- [012 Documentación de la API con OpenAPI](adr/012-documentacion-api-con-openapi.md)
- [013 Turnos](adr/013-turnos.md)
- [014 Página pública de turnos](adr/014-pagina-publica-de-turnos.md)
- [015 Límites de uso](adr/015-limites-de-uso.md)
