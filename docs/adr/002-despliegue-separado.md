# ADR 002 — Despliegue separado de frontend y backend

**Estado:** aceptada · 2026-09-25

## Contexto
Ya se dispone de un VPS y de un servidor web en un hosting estático. Es un proyecto chico, y el deploy se hace al final del Sprint 2.

## Decisión
Backend y MySQL en el VPS (Docker). Frontend estático en un hosting estático.

## Consecuencias
- Origins distintos: hay que configurar CORS y usar HTTPS en ambos.
- Como el deploy es al final, esa configuración se prueba recién ahí; dejar tiempo en el Sprint 2 para corregir.
