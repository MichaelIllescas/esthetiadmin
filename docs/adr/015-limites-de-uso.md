# ADR 015 — Límites de uso en memoria: reservas públicas y login

**Estado:** aceptada · 2026-10-03

## Contexto
La página de turnos abre el primer endpoint público que escribe datos (ADR 014), y el login acepta cualquier cantidad de contraseñas sin freno (pendiente desde la revisión del 2026-09-26). Los dos necesitan frenar a quien repite pedidos desde la misma IP. Hay una sola instancia del backend y poca RAM (ADR 011).

## Decisión
- **Un limitador propio en memoria** (`common.ratelimit.RateLimiter`), con ventana fija por clave (la IP), sin dependencias nuevas. Usa el `Clock` inyectado, así se puede testear.
- **Reservas públicas:** como máximo **5 pedidos por hora por IP** a `POST /api/public/appointments`. Cuentan todos los intentos, no solo los que reservan, para frenar también a quien prueba horarios en masa.
- **Login:** como máximo **5 intentos fallidos cada 15 minutos por IP**. Un login correcto no cuenta. Con el límite agotado se rechaza incluso la contraseña correcta, para que probar no sirva.
- **Respuesta:** 429 con código `TOO_MANY_REQUESTS` y un mensaje en español que se puede mostrar tal cual.
- **IP real:** en `prod`, `server.forward-headers-strategy=native` hace que Tomcat tome la IP de `X-Forwarded-For` que agrega nginx. Sin esto, todas las clientas compartirían la IP del proxy y se bloquearían entre ellas.
- **Memoria acotada:** las ventanas vencidas se descartan solas, y el mapa tiene un tope de claves.

## Alternativas descartadas
- **Bucket4j u otra librería:** más flexible, pero suma una dependencia para dos usos simples.
- **Bloquear por usuario en el login:** con un solo usuario (ADR 003), cualquiera podría dejar sin acceso a la responsable con solo equivocarse adrede.
- **Captcha:** suma un servicio externo y fricción. Queda como plan B (ADR 014).

## Consecuencias
- Los contadores se pierden al reiniciar el backend. Es aceptable.
- Varias personas detrás de la misma IP (por ejemplo, un wifi compartido) comparten el límite de reservas. Con 5 por hora no debería notarse.
- Si hubiera más de una instancia, el límite pasaría a necesitar un almacenamiento compartido, sin cambiar la API.
