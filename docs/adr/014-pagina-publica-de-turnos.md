# ADR 014 — Página pública de turnos en `/turnos` y API pública acotada

**Estado:** aceptada · 2026-10-01

## Contexto
Las clientas tienen que poder sacar turno sin registrarse. Hasta ahora todo el sistema requería login (ADR 003): todas las rutas de Angular, salvo `/login`, están bajo `authGuard`, y todas las de la API, salvo el login, exigen JWT. Abrir una parte al público expone el sistema a abusos (turnos falsos que llenan la agenda) y a filtrar datos personales.

## Decisión
- **Frontend:**
  - Se agrega la ruta `/turnos`, fuera del `authGuard` y del `MainLayout`, con un layout público propio, mobile first.
  - Las rutas existentes no se modifican: la raíz sigue siendo el panel (y lleva al login si no hay sesión).
  - El pie de la página pública tiene un link discreto, "Acceso administración", que lleva a `/login`.
  - La responsable comparte `esthetiadmin.example.com/turnos`, que copia desde la Agenda. La dirección sale de `environment.publicBookingUrl`, así cambia por entorno (o si algún día se usa un dominio propio) sin tocar el código.
- **Flujo público:** servicio (agrupado por categoría, **sin precio**) → día → horario → nombre, apellido y teléfono → confirmación con el resumen. Sin cuenta, sin email y sin avisos por WhatsApp.
- **API pública bajo `/api/public/**`**, con `permitAll` en `SecurityConfig`:
  - Solo expone servicios reservables (sin precio) y horarios libres. Nunca devuelve datos de otros turnos ni de clientes.
  - El único endpoint que escribe es `POST /api/public/appointments`.
- **Qué servicios se ofrecen:** `service.bookable` (por defecto `true`) permite ocultar servicios de la página pública. Además, el servicio y su categoría tienen que estar activos.
- **Protección contra abuso:**
  - Máximo **2 turnos futuros confirmados por teléfono**. El teléfono se normaliza a dígitos antes de comparar.
  - Límite de **5 reservas por hora por IP** en el `POST` público. Se implementa en memoria, sin dependencias nuevas; alcanza porque hay una sola instancia del backend (detalle en ADR 015).
  - Validación estricta de los campos:
    - teléfono de 8 a 15 dígitos;
    - nombres de hasta 100 caracteres;
    - fecha y hora dentro de la grilla.
- **IP real detrás de nginx:** el límite necesita `server.forward-headers-strategy=native` en `prod` para leer `X-Forwarded-For` (pendiente ya anotado en `progreso.md`).

## Alternativas descartadas
- **Página pública en `/` y panel en `/admin`:** la URL es más linda, pero obliga a cambiar todas las rutas, el redirect del login y los links del menú. Se puede hacer más adelante sin rehacer el componente.
- **Botón de login destacado en la cabecera:** confunde a las clientas ("¿me tengo que registrar?").
- **Captcha:** suma un servicio externo y fricción. Se deja como plan B si los límites no alcanzan.
- **Confirmación manual de turnos públicos:** la responsable prefirió que se confirmen solos.

## Consecuencias
- Primera superficie pública del sistema: cualquier endpoint nuevo bajo `/api/public` se revisa con criterio de seguridad (sin datos personales, con validación y límites).
- El límite por IP se pierde al reiniciar el backend. Es aceptable.
- Si el centro crece, el límite en memoria se puede reemplazar por uno con almacenamiento compartido sin cambiar la API.
