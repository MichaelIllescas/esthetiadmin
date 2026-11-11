# ADR 005 — Desactivar en vez de borrar cuando hay historial

**Estado:** aceptada · 2026-09-25

## Contexto
La responsable puede crear categorías o servicios por error, o dejar de ofrecer alguno. Borrarlos rompería el historial de tratamientos.

## Decisión
- **Servicio:** se puede eliminar solo si nunca se usó (sin tratamientos); si ya se usó, solo se desactiva.
- **Categoría:** se puede eliminar solo si está vacía (sin servicios); si tiene servicios, se puede desactivar (o trasladar sus servicios a otra categoría y luego eliminarla).
- Activar y desactivar es reversible.
- Los servicios de una categoría desactivada dejan de ofrecerse, sin modificar su propio estado. Al reactivar la categoría vuelven tal como estaban.

## Consecuencias
- Las entidades necesitan un indicador de activo/inactivo.
- Un servicio se ofrece si él y su categoría están activos.
- Los selectores para registrar tratamientos muestran solo lo activo; el historial y los reportes incluyen también lo inactivo.
