# ADR 007 — Frontend Angular organizado por funcionalidad

**Estado:** aceptada · 2026-09-25

## Contexto
Aplicación Angular escrita por un solo desarrollador en 2 semanas, con los mismos módulos que el backend (ver ADR 006).

## Decisión
Estructura por funcionalidad (feature-based), con componentes standalone y rutas cargadas de forma diferida (lazy loading):

- `features/<módulo>/`: `auth`, `catalog`, `clients`, `treatments`, `expenses`, `reports` (más `home`, la página de inicio). Cada uno con `pages/` (pantallas enrutables), `components/` (piezas propias), `services/` (llamadas a la API) y `models/` (interfaces de los DTO).
- `core/`: lo global y de instancia única: `guards/`, `interceptors/` (token de sesión y errores HTTP) y `services/` (`AuthTokenStore`, que guarda el token en `sessionStorage`, y `SessionService`, con el usuario actual y el cierre de sesión).
- `shared/`: `components/`, `models/` y `utils/` reutilizables entre módulos, sin dependencias de ningún feature. Hoy: `modal-dialog` (sobre el elemento nativo `<dialog>`), `confirm-dialog`, `apiErrorMessage` y `formatPrice`. Los estilos comunes (botones `.btn`, campos `.field`) están en `styles.css`.
- `layout/`: estructura visual común: `main-layout` (barra superior y navegación responsive), `user-menu` y la lista de módulos en `nav-items.ts`. Al terminar un módulo, se activa en esa lista (`enabled: true`).

Reglas:
- Un feature puede usar los servicios y componentes de otro (por ejemplo, la ficha del cliente muestra la lista de tratamientos y el formulario de tratamientos usa el buscador de clientes), siempre que **no se formen ciclos entre archivos**. Lo genérico, que no pertenece a ningún módulo, va a `shared/`.
- Los componentes no llaman a `HttpClient`: lo hacen los `services/` del feature.
- Estado local con signals; formularios reactivos.

## Consecuencias
- Cada historia de usuario toca casi siempre un solo feature, lo que ordena el trabajo por sprint.
- Los nombres de los features coinciden con los módulos del backend, lo que facilita ubicar código de punta a punta.

## Reglas para que nada se salga de la pantalla
Una cuadrícula (`display: grid`) crece por defecto hasta el ancho de su contenido más largo, así que un nombre largo sin espacios estira la página entera en el celular. Para evitarlo:
- Todo `display: grid` de una página o bloque usa `grid-template-columns: minmax(0, 1fr)` (o `repeat(auto-fit, minmax(min(220px, 100%), 1fr))`).
- Los hijos de un contenedor flex o grid que deben encogerse llevan `min-width: 0`.
- El texto de usuario (nombres, descripciones, observaciones) se parte con `overflow-wrap: anywhere`. `.content` ya lo aplica en `styles.css`.
- Las tablas usan `.data-table` (en `styles.css`): tabla en escritorio y una tarjeta por fila en celular, con `data-label` en cada celda.
- Se prueba con contenido extremo (20 letras seguidas sin espacios) a 320, 375, 800 y 1280 px.

