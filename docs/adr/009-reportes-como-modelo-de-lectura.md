# ADR 009 — Reportes como modelo de lectura con SQL agregado

**Estado:** aceptada · 2026-09-26

## Contexto
Los reportes financieros suman y agrupan tratamientos, gastos, clientes y servicios. Hacerlo cargando entidades de cada módulo y agrupando en memoria sería lento (miles de filas por período) y obligaría a que el módulo de reportes conozca las entidades de los demás.

## Decisión
El módulo `report` tiene una única clase de consultas (`ReportQueries`) que ejecuta **SQL agregado de solo lectura** (`SUM`, `COUNT`, `GROUP BY`, `JOIN`) con `JdbcClient` sobre las tablas de los otros módulos. Es una excepción acotada a la regla del ADR 006 (un módulo no toca los repositorios de otro), justificada porque un reporte es un modelo de lectura que no modifica nada.

- Toda la lógica (validar el período, completar los meses sin movimiento, calcular porcentajes) vive en `ReportService`; las consultas solo agrupan.
- El orden de los rankings sale de una lista fija en el código, nunca de un texto del usuario, para no abrir la puerta a inyección SQL.
- Cada consulta tiene un test contra la base real (con fechas de 2001 y una transacción que se deshace).

## Consecuencias
- Si cambia el nombre de una tabla o columna, el test contra la base avisa qué reporte se rompe.
- Los reportes no se cachean: con los volúmenes de un centro de estética, las consultas son instantáneas.
