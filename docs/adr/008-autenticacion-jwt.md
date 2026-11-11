# ADR 008 — Autenticación con JWT en el header Authorization

**Estado:** aceptada · 2026-09-25

## Contexto
Frontend (hosting estático) y backend (VPS) viven en dominios distintos. Hay una única usuaria (ADR 003).

## Decisión
- Login con usuario y contraseña; el backend devuelve un JWT firmado (HS256) con vencimiento de 8 horas (configurable).
- El frontend envía el token en cada request: `Authorization: Bearer <token>`.
- Sin sesión en el servidor (stateless) y sin CSRF, porque la API no usa cookies.
- Se usa el soporte propio de Spring Security (`oauth2-resource-server` con Nimbus), sin librerías de terceros.
- El secreto de firma (`JWT_SECRET`, mínimo 32 caracteres) vive solo en variables de entorno.
- CORS: únicamente los orígenes de `CORS_ALLOWED_ORIGINS`, nunca `*`.
- Las rutas de la documentación (`/swagger-ui/**`, `/v3/api-docs/**`) están permitidas sin token, pero solo existen en `dev` (ADR 012).
- Los errores 401 y 403 salen con el mismo formato JSON del resto de la API.

## Alternativa descartada
Sesión con cookie: con dominios distintos exige `SameSite=None; Secure`, CSRF y CORS con credenciales, que es donde más fallan los deploys.

## Consecuencias
- El token se guarda en el navegador; hay que evitar XSS (no renderizar HTML sin sanear).
- No hay revocación de tokens: un token robado vale hasta que vence. Aceptable para una usuaria única; si cambia la contraseña, conviene rotar `JWT_SECRET`.
