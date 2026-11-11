# ADR 004 — Importe del tratamiento: precargado y editable

**Estado:** aceptada · 2026-09-25

## Contexto
Los precios de los servicios pueden cambiar, y a veces la responsable aplica un descuento o recargo a una clienta puntual. Los reportes deben reflejar lo realmente cobrado.

## Decisión
Al registrar un tratamiento, el importe se **precarga** con el precio actual del servicio y puede **modificarse**. Se guarda como importe propio del tratamiento.

## Consecuencias
- Cambiar el precio de un servicio no altera tratamientos ya registrados.
- Los reportes de ingresos usan el importe del tratamiento, no el precio de lista.
- Se asume que el tratamiento se cobra al registrarlo (sin pagos parciales).
