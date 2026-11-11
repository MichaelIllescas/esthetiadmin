# Frontend — EsthetiAdmin

Aplicación Angular con dos partes:

- **Panel de gestión** (con login): inicio, agenda, clientes, servicios, tratamientos, horarios, gastos y reportes.
- **Página pública de turnos** en `/turnos` (sin login), pensada para celular (ADR 014).

## Desarrollo

```bash
npm install
npm start
```

Queda en `http://localhost:4200` y se recarga sola con cada cambio. Necesita el backend corriendo (ver el [README principal](../README.md)).

## Variables de entorno

En `src/environments/`: `environment.development.ts` (desarrollo) y `environment.ts` (producción).

| Variable | Descripción |
|---|---|
| `apiUrl` | URL base de la API |
| `publicBookingUrl` | Link de la página pública de turnos que la Agenda muestra para copiar |

## Tests

```bash
npm test -- --watch=false
```

Tests unitarios con Vitest: cada componente, servicio y utilidad tiene su `.spec.ts` al lado.

## Build

```bash
npm run build
```

Genera `dist/frontend/browser`. Para subirlo al hosting estático, ver la sección Despliegue del [README principal](../README.md).

## Estructura

```
src/app/
  core/       sesión, guard de rutas, interceptor (token y 401)
  layout/     layout del panel y menú
  shared/     componentes y utilidades comunes (diálogos, íconos, ayuda "¿Cómo funciona?", formatos)
  features/   una carpeta por funcionalidad: components, pages, services, models, utils
```

Criterios en el [ADR 007](../docs/adr/007-estructura-frontend.md).
