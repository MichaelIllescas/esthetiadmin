# ADR 016 — Confirmación de turnos por email y configuración del negocio

**Estado:** aceptada · 2026-10-10 · reemplaza en parte al ADR 014 ("sin email")

## Contexto
Las clientas piden recibir la confirmación de su turno, y la responsable quiere enterarse de las reservas que entran por la página pública sin tener que abrir la Agenda. Hoy el sistema no guarda ningún email: ni el turno, ni la clienta, ni el negocio. Tampoco hay un lugar donde la responsable cargue sus datos de contacto, y el mensaje de "comunicate con el centro" de la página pública no dice cómo.

## Decisión
- **Email de la clienta: opcional**, y se usa solo para avisos del turno (nada de marketing ni listados).
  - Se pide en la reserva pública y en el alta o edición de turnos del panel.
  - Se guarda en el turno (`appointment.email`). Si el turno está vinculado a una clienta que no tiene email, se le copia a la ficha (`client.email`). Uno ya cargado nunca se pisa.
  - Al crear un turno del panel para una clienta con email, el formulario lo precarga.
- **Cuándo se manda cada email:**

  | Caso | A la clienta | A la responsable |
  |---|---|---|
  | Reserva pública | Automático, si cargó email | Automático, al email de Configuración |
  | Alta, reprogramación o cancelación desde el panel | Solo si la responsable confirma "¿Enviar email?" | No |

  - Si el turno no tiene email, el panel muestra la opción deshabilitada ("La clienta no tiene email cargado") y la API rechaza el envío (400). La regla vive en el backend, no solo en la pantalla.
  - Desde el detalle del turno se puede reenviar el último aviso, con la misma regla.
- **Configuración del negocio** (sección nueva del panel, una sola fila `business_settings`):
  - **Email de avisos:** dónde recibe la responsable las reservas públicas. Nunca se expone en la API pública.
  - **Celular de contacto:** el que usan las clientas para cancelar o reagendar. Se normaliza con `PhoneNumbers`. Aparece en todos los emails a la clienta, en la confirmación de la reserva pública y en el mensaje de límite de turnos, con un link a WhatsApp (`wa.me`). Lo expone `GET /api/public/contact`, que devuelve solo el celular.
  - Si un dato no está cargado, se omite la línea o el aviso correspondiente en vez de mostrar algo vacío.
- **Envío:**
  - SMTP del proveedor de hosting con `spring-boot-starter-mail`. Host, puerto, usuario, contraseña y remitente van en el `.env` del VPS (`MAIL_*`), nunca en la base ni en la pantalla de Configuración.
  - `notifications.email.enabled` permite apagarlo (apagado en `dev` y en los tests).
  - Plantillas HTML con Thymeleaf: una para la clienta, con el asunto y el texto según el tipo (confirmado, reprogramado o cancelado), y otra para el nuevo turno de la responsable. Los datos de la clienta se escapan.
  - El link de WhatsApp agrega `549` a los celulares locales de 10 dígitos.
  - En la reserva pública el envío se dispara con un evento después del commit y en otro hilo (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`): no se manda nada si la reserva falla, la clienta no espera al SMTP y una falla de envío no rompe la reserva.
  - En el panel el envío es una acción aparte (`POST /api/appointments/{id}/notifications`). Si el SMTP falla, el turno ya quedó guardado y la respuesta lo dice para que la responsable reintente.
  - Los logs registran el id del turno y el tipo de aviso, nunca el email.

## Alternativas descartadas
- **Email obligatorio:** suma fricción a la reserva y no todas las clientas lo usan.
- **Envío automático también desde el panel:** la responsable quiere decidir en cada caso (por ejemplo, no avisar un cambio que ya habló por teléfono).
- **Flag `sendEmail` dentro del alta o edición del turno:** mezcla guardar con avisar y no sirve para reenviar.
- **Servicio transaccional externo (Brevo, Resend, etc.):** el proveedor de hosting ya está contratado y el dominio es suyo.
- **Credenciales SMTP editables desde el panel:** guardar una contraseña en la base no aporta y suma riesgo.
- **WhatsApp automático:** requiere la API de WhatsApp Business. Se queda en el link manual.

## Consecuencias
- El sistema empieza a guardar emails de clientas: es un dato personal más, con el mismo cuidado que el teléfono.
- Que los emails no caigan en spam depende de SPF y DKIM en del dominio (tarea de infraestructura, fuera del código).
- Una falla de envío en la reserva pública solo queda en el log: no hay reintentos ni cola. Es aceptable por el volumen; si hiciera falta, se agrega una tabla de envíos pendientes sin cambiar la API.
- El backend hace una conexión saliente al SMTP: hay que verificar que el VPS permita el puerto (587 o 465).
