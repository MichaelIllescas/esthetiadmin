# ADR 003 — Usuario único precargado, con login

**Estado:** aceptada · 2026-09-25

## Contexto
La única usuaria es la responsable. El sistema guarda datos personales de clientas y estará expuesto en internet.

## Decisión
Login con usuario y contraseña (cifrada). Un único usuario precargado en la base. Sin registro público ni recuperación de contraseña.

## Consecuencias
- Se descartan roles y gestión de usuarios.
- Si necesita cambiar la contraseña, el desarrollador lo hace directo en la base.
