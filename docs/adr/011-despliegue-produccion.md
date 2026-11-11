# ADR 011 — Despliegue en producción: imagen en Docker Hub, MySQL compartido y frontend en un hosting estático

**Estado:** aceptada · 2026-09-27

## Contexto
El VPS ya aloja otras aplicaciones, con nginx, Docker y tres instancias de MySQL, y tiene poca RAM y poco disco. Levantar un MySQL propio para EsthetiAdmin sumaría consumo sin necesidad. El ADR 002 dejó pendiente cómo se hacía el deploy.

## Decisión
- **Backend:** imagen construida con el `Dockerfile` de `backend/` (build en dos etapas, JRE 25 sobre Alpine, usuario sin privilegios), subida a Docker Hub como `jonathanillescas/esthetiadmin-backend:<versión>` y `latest`. En el VPS corre con `docker compose` usando siempre el tag de versión, no `latest`, para poder volver atrás.
- **Memoria:** heap de 64 a 200 MB, `SerialGC` y metaspace limitado; el contenedor tiene un tope de 450 MB (`mem_limit` y `memswap_limit`).
- **Base de datos:** se reutiliza el MySQL del host (puerto 3306) con una base `esthetiadmin` y un usuario `esthetiadmin` con permisos solo sobre esa base. El contenedor usa `network_mode: host` porque ese MySQL escucha solo en `127.0.0.1`; el backend escucha en `127.0.0.1:8082` (`SERVER_ADDRESS` y `SERVER_PORT`) y no queda expuesto.
- **HTTPS:** nginx del VPS como proxy inverso hacia `127.0.0.1:8082`, con certificado de Let's Encrypt (certbot, renovación automática), en `api.esthetiadmin.example.com`.
- **Frontend:** build estático (`npm run build`) subido al hosting estático en `esthetiadmin.example.com`. La carpeta `dist` se sube tal cual; un `.htaccess` en la raíz del subdominio (`frontend/deploy/.htaccess`) fuerza HTTPS, sirve `dist/frontend/browser` y manda las rutas de Angular a `index.html`.
- **Configuración:** las variables de `prod` viven en un `.env` que solo existe en el VPS (`chmod 600`). No entra en la imagen (`.dockerignore`).
- **Documentación de la API:** desactivada en `prod` (`springdoc.*.enabled=false`), para no exponer el mapa de la API (ADR 012).
- **Zona horaria:** fija en `America/Argentina/Buenos_Aires` en la imagen (`TZ` y `-Duser.timezone`).

## Consecuencias
- Los datos no están en volúmenes del backend: viven en MySQL, y el backend no guarda archivos (JWT sin estado). Recrear el contenedor no pierde nada.
- Comparte instancia de MySQL con otra aplicación: un reinicio o una caída afecta a ambas, y los backups son de la instancia completa.
- El usuario inicial se crea solo si no existe ningún usuario. Cambiar `ADMIN_USERNAME` o `ADMIN_PASSWORD` en el `.env` no modifica uno ya creado: hay que borrar la fila de `app_user` y recrear el contenedor con `--force-recreate`.
- Para actualizar: construir y subir una imagen nueva, cambiar el tag en el `docker-compose.yml` del VPS y ejecutar `docker compose up -d`.
