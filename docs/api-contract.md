# Contrato de la API

## Convenciones
- Prefijo `/api`, rutas en inglés y en plural: `/api/clients`, `/api/treatments`.
- Campos en `camelCase`. Fechas ISO (`2026-09-25`). Importes como número (`25000.00`).
- Respuestas exitosas **sin envoltorio**: se devuelve el recurso directamente.

| Operación | HTTP | Cuerpo |
|---|---|---|
| Consultar uno o listar | 200 | Recurso o arreglo |
| Crear | 201 | Recurso creado |
| Editar | 200 | Recurso actualizado |
| Eliminar | 204 | Vacío |

## Autenticación
Todas las rutas requieren `Authorization: Bearer <token>`, salvo el login, la API pública de turnos (`/api/public/**`, ADR 014) y, solo en `dev`, la documentación (`/swagger-ui.html`, `/v3/api-docs`).

`POST /api/auth/login` (público)

```json
// request
{ "username": "admin", "password": "..." }
// response 200
{ "token": "eyJ...", "expiresAt": "2026-09-26T04:00:00Z" }
```

Credenciales incorrectas: 401 con `Usuario o contraseña incorrectos` (el mismo mensaje si falla el usuario o la contraseña).

Más de 5 intentos fallidos en 15 minutos desde la misma IP: 429 `Demasiados intentos fallidos. Esperá unos minutos y volvé a intentar`, aun con la contraseña correcta (ADR 015).

## Catálogo

| Operación | Endpoint | Respuesta |
|---|---|---|
| Listar categorías con sus servicios | `GET /api/categories` | 200 |
| Crear categoría | `POST /api/categories` | 201 |
| Modificar categoría (nombre y duración de los turnos) | `PUT /api/categories/{id}` | 200 |
| Activar o desactivar categoría | `PATCH /api/categories/{id}/active` | 200 |
| Eliminar categoría (solo vacía) | `DELETE /api/categories/{id}` | 204 |
| Crear servicio | `POST /api/services` | 201 |
| Editar servicio (incluye cambiarlo de categoría) | `PUT /api/services/{id}` | 200 |
| Activar o desactivar servicio | `PATCH /api/services/{id}/active` | 200 |
| Eliminar servicio (solo si nunca se usó) | `DELETE /api/services/{id}` | 204 |

```json
// GET /api/categories
[{ "id": 1, "name": "Depilación láser", "active": true, "appointmentMinutes": 30,
   "services": [{ "id": 10, "categoryId": 1, "name": "Bozo", "description": null, "price": 15000.00,
                  "active": true, "bookable": true }] }]
// POST/PUT /api/categories  →  { "name": "Masajes", "appointmentMinutes": 60 }
// POST/PUT /api/services    →  { "categoryId": 1, "name": "Bozo", "description": null, "price": 15000.00, "bookable": true }
// PATCH .../active          →  { "active": false }
```

Reglas: `appointmentMinutes` solo acepta 30 o 60 (400 si no); `bookable` es booleano. Si se omiten, al crear valen 60 y `true`, y al modificar se conserva el valor actual (ADR 013 y 014). Nombre único (categoría) y único dentro de su categoría (servicio) → 409. Eliminar una categoría con servicios o un servicio con tratamientos → 409 con el motivo. Los servicios de una categoría desactivada dejan de ofrecerse, sin modificar su propio estado (se aplica al registrar tratamientos).

## Clientes

| Operación | Endpoint | Respuesta |
|---|---|---|
| Buscar y listar (paginado) | `GET /api/clients?search=&page=0&size=20` | 200 |
| Ver un cliente | `GET /api/clients/{id}` | 200 |
| Registrar | `POST /api/clients` | 201 |
| Editar | `PUT /api/clients/{id}` | 200 |
| Eliminar (solo sin tratamientos) | `DELETE /api/clients/{id}` | 204 |
| ¿Existe un homónimo? | `GET /api/clients/exists?firstName=&lastName=&excludeId=` | 200 |
| Cumpleaños próximos | `GET /api/clients/birthdays?days=30` | 200 |

```json
// cliente
{ "id": 7, "firstName": "Carina", "lastName": "Diaz", "phone": "3415551234", "email": "carina@mail.com",
  "birthDate": "1985-11-03", "age": 40, "notes": "Piel sensible" }
// POST/PUT (firstName y lastName obligatorios; el resto opcional)
{ "firstName": "Carina", "lastName": "Diaz", "phone": null, "email": null, "birthDate": "1985-11-03", "notes": null }
// GET /exists
{ "exists": true }
// GET /birthdays
[{ "id": 7, "firstName": "Carina", "lastName": "Diaz", "birthDate": "1985-11-03",
   "nextBirthday": "2026-11-03", "turningAge": 41, "daysUntil": 39 }]
```

Reglas: la edad se calcula (no se guarda; `null` sin fecha de nacimiento). La fecha de nacimiento no puede ser futura. La búsqueda ignora mayúsculas y tildes, y cada palabra debe aparecer en el nombre o el apellido; orden por apellido y nombre; `size` entre 1 y 100. Eliminar un cliente con tratamientos → 409. `birthdays`: `days` entre 1 y 366; quien nació un 29 de febrero se festeja el 28 en años no bisiestos. El aviso de homónimo lo resuelve el frontend con `exists`: el backend nunca rechaza un homónimo.

## Tratamientos

| Operación | Endpoint | Respuesta |
|---|---|---|
| Registrar | `POST /api/treatments` | 201 |
| Editar | `PUT /api/treatments/{id}` | 200 |
| Eliminar | `DELETE /api/treatments/{id}` | 204 |
| Listar / historial (paginado) | `GET /api/treatments?clientId=&serviceId=&categoryId=&from=&to=&page=0&size=20` | 200 |

```json
// tratamiento
{ "id": 31, "clientId": 7, "clientName": "Diaz, Carina",
  "serviceId": 10, "serviceName": "Bozo", "categoryId": 1, "categoryName": "Depilación láser",
  "performedOn": "2026-09-25", "amount": 15000.00, "notes": "Sin reacciones" }
// POST  →  { "clientId": 7, "serviceId": 10, "performedOn": "2026-09-25", "amount": 15000.00, "notes": null }
// PUT   →  { "serviceId": 10, "performedOn": "2026-09-25", "amount": 12000.00, "notes": "Con descuento" }
```

Reglas: cliente, servicio, fecha e importe son obligatorios. La fecha no puede ser futura. El importe es de **0 en adelante** (un tratamiento gratis es válido), con hasta 2 decimales; el frontend lo precarga con el precio del servicio y es editable (ADR 004). Solo se registran servicios **activos y de una categoría activa** (409 si no); al editar se puede conservar el propio servicio aunque después se haya desactivado. El cliente no se puede cambiar al editar. Historial ordenado del más reciente al más antiguo; todos los filtros son opcionales y combinables, `from` y `to` incluyen ambos días (400 si `from` es posterior a `to`). La ficha del cliente lo usa con `?clientId=`.

## Gastos

| Operación | Endpoint | Respuesta |
|---|---|---|
| Listar con total del período | `GET /api/expenses?from=&to=&expenseTypeId=` | 200 |
| Registrar | `POST /api/expenses` | 201 |
| Editar | `PUT /api/expenses/{id}` | 200 |
| Eliminar | `DELETE /api/expenses/{id}` | 204 |
| Tipos de gasto (lista fija) | `GET /api/expense-types` | 200 |

```json
// GET /api/expenses  (sin paginar: devuelve todo el período pedido)
{ "content": [{ "id": 5, "spentOn": "2026-09-20", "description": "Alquiler de septiembre",
                "amount": 350000.00, "expenseTypeId": 1, "expenseTypeName": "Alquiler" }],
  "totalAmount": 350000.00 }
// POST/PUT
{ "spentOn": "2026-09-20", "description": "Alquiler de septiembre", "amount": 350000.00, "expenseTypeId": 1 }
// GET /api/expense-types
[{ "id": 1, "name": "Alquiler" }]
```

Reglas: fecha, concepto, importe y tipo son obligatorios. La fecha no puede ser futura. El importe es **mayor que cero** (a diferencia de los tratamientos, un gasto de 0 no tiene sentido). Los tipos son una lista fija cargada por migración (sin pantalla para gestionarlos; siempre existe "Varios"). Todos los filtros son opcionales y combinables; `from` y `to` incluyen ambos días (400 si `from` es posterior a `to`; 404 si el tipo no existe).

## Reportes

Todos reciben el período con `from` y `to` (formato `2026-09-01`, **ambos días incluidos**) y son de solo lectura.

| Reporte | Endpoint |
|---|---|
| Resumen del período | `GET /api/reports/summary?from=&to=` |
| Evolución mensual | `GET /api/reports/monthly?from=&to=` |
| Servicios más realizados | `GET /api/reports/top-services?from=&to=&limit=10` |
| Ingresos por servicio y categoría | `GET /api/reports/income-by-service?from=&to=` |
| Ranking de clientes | `GET /api/reports/top-clients?from=&to=&limit=10&sortBy=amount` |

```json
// summary
{ "from": "2026-09-01", "to": "2026-09-30", "income": 8800.00, "expenses": 6100.00,
  "balance": 2700.00, "treatmentsCount": 8, "expensesCount": 3 }
// monthly  (incluye los meses sin movimiento, con ceros)
[{ "month": "2026-09", "income": 4500.00, "expenses": 800.00, "balance": 3700.00 }]
// top-services  (más realizados primero)
[{ "serviceId": 10, "serviceName": "Bozo", "categoryId": 1, "categoryName": "Depilación láser",
   "treatments": 6, "income": 2800.00 }]
// income-by-service  (categorías de mayor a menor ingreso; adentro, sus servicios)
{ "totalIncome": 8800.00,
  "categories": [{ "categoryId": 1, "categoryName": "Depilación láser", "income": 4800.00, "percentage": 54.55,
                   "treatments": 7,
                   "services": [{ "serviceId": 10, "serviceName": "Bozo", "income": 2800.00,
                                  "percentage": 31.82, "treatments": 6 }] }] }
// top-clients
[{ "clientId": 7, "clientName": "Diaz, Carina", "treatments": 3, "totalAmount": 7000.00 }]
```

Reglas:
- **Ingresos** = suma del importe de los tratamientos (por su fecha, `performedOn`). **Gastos** = suma de los gastos (por `spentOn`). **Resultado** = ingresos − gastos, y puede ser negativo.
- Un tratamiento **gratis** cuenta como tratamiento pero suma 0 al ingreso.
- Los servicios y categorías **desactivados** siguen apareciendo si tuvieron tratamientos en el período.
- `percentage` es la parte del ingreso total del período, con 2 decimales (redondeados, pueden no sumar 100,00 exacto).
- `top-services`: ordenado por cantidad de tratamientos, luego por ingreso y luego por nombre. `top-clients`: `sortBy=amount` (por defecto, importe total) o `sortBy=treatments` (cantidad de tratamientos).
- `limit`: entre 1 y 100 (10 por defecto).
- El período es obligatorio, `from` no puede ser posterior a `to` y no puede superar los **5 años**; si no, 400 con el motivo.

## Turnos (panel)

Ver ADR 013. Fechas y horas en la zona del centro, sin desplazamiento (`2026-10-08T10:30:00`).

| Operación | Endpoint | Respuesta |
|---|---|---|
| Listar la agenda de un rango | `GET /api/appointments?from=&to=&status=` | 200 |
| Ver un turno | `GET /api/appointments/{id}` | 200 |
| Cargar turno | `POST /api/appointments` | 201 |
| Reprogramar o corregir datos | `PUT /api/appointments/{id}` | 200 |
| Cambiar estado | `PATCH /api/appointments/{id}/status` | 200 |
| Vincular con un cliente | `PATCH /api/appointments/{id}/client` | 200 |
| Enviar aviso por email a la clienta | `POST /api/appointments/{id}/notifications` | 204 |
| Horarios libres (panel) | `GET /api/appointments/availability?serviceId=&date=&excludeAppointmentId=` | 200 |
| Horarios de atención | `GET /api/availability-rules` · `PUT /api/availability-rules` | 200 |
| Listar bloqueos | `GET /api/availability-blocks?from=&to=` | 200 |
| Crear bloqueo | `POST /api/availability-blocks` | 201 |
| Eliminar bloqueo | `DELETE /api/availability-blocks/{id}` | 204 |

```json
// turno
{ "id": 52, "serviceId": 10, "serviceName": "Bozo", "categoryName": "Depilación",
  "startsAt": "2026-10-08T10:30:00", "endsAt": "2026-10-08T11:00:00",
  "firstName": "Carina", "lastName": "Diaz", "phone": "3415551234", "email": "carina@mail.com",
  "clientId": null, "status": "CONFIRMED", "source": "PUBLIC", "notes": null }
// POST/PUT  (clientId opcional; si viene, nombre, apellido, teléfono y email se toman del cliente cuando falten)
{ "serviceId": 10, "startsAt": "2026-10-08T10:30:00", "clientId": null,
  "firstName": "Carina", "lastName": "Diaz", "phone": "3415551234", "email": null, "notes": null }
// POST .../notifications  (BOOKED | RESCHEDULED | CANCELLED)
{ "type": "RESCHEDULED" }
// PATCH .../status  →  { "status": "ATTENDED" }      // CANCELLED | ATTENDED | NO_SHOW
// PATCH .../client  →  { "clientId": 7 }
// GET /api/appointments/availability
{ "date": "2026-10-08", "durationMinutes": 60, "times": ["09:00", "09:30", "11:00"] }
// GET/PUT /api/availability-rules  (el PUT reemplaza la semana completa)
[{ "dayOfWeek": 1, "startTime": "09:00", "endTime": "13:00" },
 { "dayOfWeek": 1, "startTime": "15:00", "endTime": "19:00" }]
// POST /api/availability-blocks  (sin horas = días completos; un solo día: startDate = endDate)
{ "startDate": "2027-01-04", "endDate": "2027-01-15", "startTime": null, "endTime": null, "reason": "Vacaciones" }
{ "startDate": "2026-10-16", "endDate": "2026-10-16", "startTime": "15:00", "endTime": "19:00", "reason": null }
// respuesta del bloqueo: incluye los turnos activos que quedan dentro, para avisar
{ "id": 3, "startDate": "2026-10-12", "endDate": "2026-10-12", "startTime": null, "endTime": null, "reason": "Feriado",
  "affectedAppointments": [{ "id": 52, "startsAt": "2026-10-12T10:30:00", "firstName": "Carina", "lastName": "Diaz" }] }
```

Reglas:
- `startsAt` en :00 o :30, no en el pasado. La duración sale de la categoría del servicio. El servicio tiene que estar activo, igual que su categoría (no hace falta que sea `bookable`).
- La carga desde el panel **puede** quedar fuera del horario de atención, de un bloqueo o del tope de 60 días. **No puede** superponerse con otro turno activo: en ese caso responde 409 con `Ese horario ya está ocupado`.
- `GET /api/appointments/availability` aplica las mismas reglas que la pública (horario de atención, bloqueos y turnos), pero sin el tope de 60 días. `excludeAppointmentId` (opcional) libera el horario de ese turno, para mostrarlo como libre al reprogramarlo.
- `from`/`to` son obligatorios, se incluyen ambos días y el rango no puede superar 62 días. Orden por `startsAt`.
- Al reprogramar se puede conservar el propio servicio aunque se haya desactivado después; para cambiarlo, el nuevo tiene que estar activo.
- No se puede eliminar un cliente ni un servicio que tenga turnos (409).
- Transiciones de estado:
  - solo desde `CONFIRMED`;
  - `CANCELLED` y `NO_SHOW` liberan el horario;
  - `ATTENDED` exige `clientId` (409 `Vinculá el turno con un cliente antes de marcarlo atendido`).
- `PUT` solo sobre turnos `CONFIRMED`.
- Email (ADR 016):
  - Opcional, hasta 254 caracteres y con formato válido; se guarda en minúsculas.
  - Al vincular el turno con un cliente sin email (al crearlo con `clientId` o con `PATCH .../client`), se le copia. Uno ya cargado nunca se pisa.
- Avisos (`POST .../notifications`):
  - Nunca se mandan solos desde el panel: el frontend pregunta y llama a este endpoint si la responsable acepta.
  - Sin email en el turno: 400 `La clienta no tiene email cargado`.
  - `BOOKED` y `RESCHEDULED` solo para turnos `CONFIRMED`; `CANCELLED` solo para turnos `CANCELLED` (409 si no).
  - Si el SMTP falla: 502 `EMAIL_NOT_SENT` (`No se pudo enviar el email. Probá de nuevo más tarde`). El turno no cambia.
- Horarios de atención:
  - `dayOfWeek` va de 1 (lunes) a 7 (domingo);
  - las horas son múltiplos de 30 minutos y `startTime` < `endTime`;
  - los rangos de un mismo día no se superponen (400).
- Bloqueos:
  - `startDate` no puede ser pasada y `endDate` no puede ser anterior a `startDate`; como máximo 366 días.
  - Horas las dos o ninguna, en múltiplos de 30 minutos, y solo en un bloqueo de un día (400 si no).
  - `GET` devuelve los bloqueos que se cruzan con el rango pedido. Sin `from`, arranca hoy (los que ya terminaron no aparecen); sin `to`, no tiene tope.
  - Crear un bloqueo no cancela turnos.

## Turnos (públicos)

Sin autenticación (ADR 014). Nunca devuelven precios ni datos de otros turnos.

| Operación | Endpoint | Respuesta |
|---|---|---|
| Servicios reservables | `GET /api/public/services` | 200 |
| Días con lugar en un mes | `GET /api/public/availability/days?serviceId=&month=2026-10` | 200 |
| Horarios libres de un día | `GET /api/public/availability?serviceId=&date=` | 200 |
| Reservar | `POST /api/public/appointments` | 201 |
| Celular de contacto | `GET /api/public/contact` | 200 |

```json
// GET /api/public/services  (solo categorías y servicios activos y bookable; sin precio)
[{ "categoryId": 1, "categoryName": "Depilación", "durationMinutes": 30,
   "services": [{ "id": 10, "name": "Bozo", "description": null }] }]
// GET /api/public/availability/days
{ "month": "2026-10", "days": ["2026-10-06", "2026-10-07", "2026-10-09"] }
// GET /api/public/availability
{ "date": "2026-10-08", "durationMinutes": 30, "times": ["09:00", "09:30", "10:00"] }
// POST /api/public/appointments
{ "serviceId": 10, "startsAt": "2026-10-08T10:30:00",
  "firstName": "Carina", "lastName": "Diaz", "phone": "341 555-1234", "email": null }
// response 201  (solo el resumen, sin id interno ni datos de otros)
{ "serviceName": "Bozo", "startsAt": "2026-10-08T10:30:00", "endsAt": "2026-10-08T11:00:00",
  "firstName": "Carina", "emailSent": true }
// GET /api/public/contact  (solo el celular; null si no está cargado)
{ "phone": "3415559876" }
```

Reglas:
- Solo se ofrecen fechas desde hoy (horarios no pasados) hasta **60 días** adelante; fuera de ese rango, 400.
- El horario tiene que estar libre según el horario de atención, los bloqueos y los turnos activos. Si se ocupó mientras la persona completaba los datos: 409 `Ese horario acaba de ocuparse, elegí otro`.
- Nombre y apellido son obligatorios (hasta 100 caracteres). El teléfono es obligatorio: se guardan solo sus dígitos, que tienen que ser entre 8 y 15.
- Con 2 turnos futuros `CONFIRMED` para el mismo teléfono: 409 `Ya tenés 2 turnos reservados. Para otro, comunicate con el centro`, seguido de `al <celular>` si está cargado en Configuración.
- Más de 5 pedidos de reserva por hora desde la misma IP: 429 `Hiciste demasiados pedidos de turno. Probá de nuevo más tarde` (ADR 015).
- El turno se crea `CONFIRMED`, con `source = PUBLIC`.
- El email es opcional, con las mismas reglas que en el panel (ADR 016). `emailSent` indica si se pidió la confirmación (hay email y los avisos están habilitados); el envío sale después de guardar el turno, y si falla, la reserva queda igual.
- Cada reserva avisa por email a la responsable, si hay email de avisos en Configuración.

## Configuración

Ver ADR 016. Una sola configuración del negocio.

| Operación | Endpoint | Respuesta |
|---|---|---|
| Ver | `GET /api/settings` | 200 |
| Guardar | `PUT /api/settings` | 200 |

```json
// GET y PUT (los dos campos son opcionales; null los borra)
{ "notificationEmail": "esthetiadmin@mail.com", "contactPhone": "3415559876" }
```

Reglas: `notificationEmail` con formato válido, hasta 254 caracteres. `contactPhone` se guarda normalizado (8 a 15 dígitos). Las credenciales SMTP no se configuran desde la API.

## Listas paginadas
DTO propio `PageResponse` (no se serializa el `Page` de Spring):

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 134, "totalPages": 7 }
```

## Errores
Formato único para todos los errores. `message` va en español y se puede mostrar tal cual a la usuaria; `code` es estable y lo usa el frontend para decidir qué hacer.

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "El nombre es obligatorio; La fecha no puede ser futura",
  "path": "/api/clients",
  "timestamp": "2026-09-25T21:50:15-03:00"
}
```

Sin detalle por campo: si hay varios errores de validación, se unen en `message` separados por `; `.

| Situación | HTTP | `code` |
|---|---|---|
| Datos inválidos | 400 | `VALIDATION_ERROR` |
| No autenticada / credenciales incorrectas | 401 | `UNAUTHORIZED` |
| Sin permisos | 403 | `FORBIDDEN` |
| Recurso inexistente | 404 | `NOT_FOUND` |
| Petición mal formada o no soportada (JSON inválido, método no permitido) | 400 / 405 | `BAD_REQUEST` |
| Regla de negocio violada (borrar cliente con tratamientos, nombre duplicado, horario ocupado) | 409 | `CONFLICT` |
| Demasiados pedidos desde la misma IP (reservas públicas, login fallido) | 429 | `TOO_MANY_REQUESTS` |
| No se pudo enviar un email desde el panel | 502 | `EMAIL_NOT_SENT` |
| Error inesperado | 500 | `INTERNAL_ERROR` (mensaje genérico, sin detalles internos) |
