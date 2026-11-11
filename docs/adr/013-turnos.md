# ADR 013 — Turnos: grilla de 30 minutos, duración por categoría y casilleros únicos

**Estado:** aceptada · 2026-10-01

## Contexto
La responsable pidió un gestor de turnos: ella configura los días y horarios en que atiende, las clientas sacan turno desde una página pública (ADR 014) y ella también puede cargarlos. Atiende **a una sola persona a la vez**. Los turnos de depilación duran media hora y los del resto de los servicios, una hora. La visión original dejaba los turnos fuera de alcance; este ADR los incorpora.

## Decisión
- **Turno ≠ tratamiento.** El turno es una reserva a futuro, sin importe (tabla `appointment`). El tratamiento sigue siendo lo realizado y cobrado (ADR 004). Un turno no genera ingreso: recién cuando se marca *atendido* se ofrece registrar el tratamiento con el servicio y la fecha precargados. Así los reportes no cambian.
- **Duración por categoría.** `category.appointment_minutes` acepta 30 o 60, con 60 por defecto. La migración pone 30 en la categoría "Depilación" (búsqueda por nombre; si no existe, no hace nada). Todos los servicios heredan la duración de su categoría.
- **Grilla fija de 30 minutos.** Los turnos empiezan en :00 o :30. Uno de 30 minutos ocupa 1 casillero y uno de 60 ocupa 2 consecutivos. Los horarios de atención y los bloqueos también se cargan en múltiplos de 30 minutos.
- **Agenda configurable:**
  - `availability_rule`: rangos por día de la semana; puede haber varios por día.
  - `availability_block`: excepciones por **rango de fechas** (desde/hasta, ambos incluidos). Sin horas, bloquea los días completos. Las horas solo se permiten en un bloqueo de un solo día. Un bloqueo de varios días con horas ("todas las tardes de esta semana") es poco común y complicaría el formulario; se resuelve con un bloqueo por día.
- **Disponibilidad calculada, no guardada.** Un horario está libre si cumple todo esto:
  - está dentro de un rango de atención;
  - termina antes del fin de ese rango;
  - no se superpone con un bloqueo ni con otro turno no cancelado;
  - no está en el pasado;
  - para la reserva pública, cae dentro de los próximos **60 días**.
  - El reloj es el `Clock` inyectado, con la zona de Buenos Aires (ADR 011).
- **Sin superposición garantizada por la base.** Cada turno activo ocupa una o dos filas en `appointment_slot`, una por casillero, con `UNIQUE (slot_start)`. Si dos personas confirman el mismo horario a la vez, la segunda falla por la restricción y recibe un 409. Al cancelar o marcar *ausente*, las filas se borran y el horario queda libre. Funciona porque se atiende de a una persona.
- **Estados:** `CONFIRMED` (estado inicial; los públicos se confirman solos), `CANCELLED`, `ATTENDED` y `NO_SHOW`.
- **Datos de la persona en el turno.** Nombre, apellido y teléfono se guardan en el turno, sin crear un cliente. `client_id` es opcional: se completa cuando la responsable lo vincula con un cliente existente o crea uno nuevo, y es obligatorio para marcarlo *atendido*.
- **Carga desde el panel.** Respeta la grilla, la no superposición y el "no en pasado". Puede quedar fuera del horario de atención, de un bloqueo o del tope de 60 días, porque la responsable decide su agenda.

## Alternativas descartadas
- **Duración por servicio:** es más flexible, pero obliga a configurarla en cada servicio. La regla de la responsable es por tipo de servicio.
- **Inicio libre (el turno empieza cuando termina el anterior):** aprovecha mejor huecos de 30 minutos, pero complica la grilla y la lectura de la agenda.
- **Validar la superposición solo en el servicio:** dos pedidos simultáneos pueden pasar ambos la validación. MySQL no tiene restricciones de exclusión por rango.
- **Usar `treatment` con fecha futura:** inflaría los reportes con turnos que tal vez no ocurran.

## Consecuencias
- Cambiar la duración de una categoría o los horarios de atención no mueve los turnos ya tomados (`ends_at` y sus casilleros quedan fijos).
- Crear un bloqueo sobre turnos existentes no los cancela: la pantalla avisa cuáles quedan afectados para que ella decida.
- Unas vacaciones son un solo bloqueo, no uno por día, así la lista de bloqueos queda corta sin paginar. La lista muestra solo los bloqueos que terminan de hoy en adelante.
- Los horarios de atención se repiten todas las semanas hasta que la responsable los cambia. No hay "horario especial" para abrir un día puntual que normalmente está cerrado: queda para una etapa posterior.
- Si en el futuro hubiera dos profesionales, habría que agregar un recurso a `appointment_slot` (`UNIQUE (resource_id, slot_start)`).
