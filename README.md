# EsthetiAdmin — gestión para centros de estética

Sistema de gestión para centros de estética: permite organizar clientes, administrar el catálogo de servicios por categorías, gestionar la agenda de turnos, registrar los tratamientos realizados, llevar los gastos y consultar reportes financieros.

- **Panel de gestión:** uso interno, con login. Usuaria única: la responsable del centro.
- **Página pública de turnos (`/turnos`):** las clientas sacan turno sin registrarse (ADR 014).

## Módulos

- **Agenda:** turnos por día y por semana; alta, reprogramación, cancelación, "no vino" y paso a tratamiento al marcarlo atendido. Incluye el link para copiar y pasarles a las clientas.
- **Horarios:** horarios de atención semanales y bloqueos por rango de fechas (feriados, vacaciones).
- **Reserva pública:** servicio → día → hora → nombre, apellido y teléfono. Turnos de 30 minutos o 1 hora según la categoría, sin superposición, hasta 60 días adelante, con límites contra abuso (ADR 013 a 015).
- **Clientes:** ficha, búsqueda, historial de tratamientos y cumpleaños.
- **Servicios y categorías:** catálogo administrable, con la duración de los turnos por categoría y qué servicios se reservan online.
- **Tratamientos:** registro de lo realizado a cada cliente, con importe editable.
- **Gastos:** carga y consulta por período y tipo.
- **Reportes:** ingresos, gastos, resultado, evolución mensual, servicios más realizados, ingresos por servicio y categoría, y ranking de clientes.

## Stack

| Capa | Tecnología |
|---|---|
| Frontend | Angular |
| Backend | Java 25, Spring Boot 4, Maven |
| Base de datos | MySQL, migraciones con Flyway |
| Documentación de la API | OpenAPI con springdoc (Swagger UI), solo en `dev` |
| Despliegue | Backend en Docker (imagen en Docker Hub) sobre un VPS con nginx y MySQL, frontend estático en un hosting estático |

## Estructura

```
backend/    API REST (Spring Boot)
frontend/   Aplicación web (Angular)
docs/       Visión, backlog, sprints, modelo de datos y decisiones (ADR)
```

## Puesta en marcha (backend)

Requisitos: JDK 25 y MySQL en ejecución.

1. Copiar la plantilla de variables de entorno y completarla:
   ```bash
   cd backend
   cp .env.example .env
   ```
2. Levantar la aplicación (la base `esthetiadmin` y las tablas se crean solas en el perfil `dev`):
   ```bash
   ./mvnw spring-boot:run
   ```

### Documentación interactiva de la API

Con el perfil `dev`, Swagger UI está en `http://localhost:8080/swagger-ui.html` (puerto por defecto). Para probar los endpoints protegidos: ejecutar `POST /api/auth/login`, copiar el `token` y pegarlo en el botón **Authorize**. En `prod` está desactivada (ADR 012).

### Variables de entorno

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` (por defecto) o `prod` |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Conexión a MySQL |
| `DB_USER`, `DB_PASSWORD` | Credenciales de la base |
| `JWT_SECRET` | Secreto para firmar tokens (mínimo 32 caracteres) |
| `JWT_EXPIRATION_MINUTES` | Duración del token en minutos (480 = 8 horas) |
| `CORS_ALLOWED_ORIGINS` | Orígenes del frontend permitidos, separados por coma |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Usuario inicial (contraseña de 8+ caracteres). Se crea al arrancar solo si no existe ningún usuario |
| `MAIL_ENABLED` | `true` para enviar los emails de turnos (ADR 016); `false` por defecto |
| `MAIL_HOST`, `MAIL_PORT` | Servidor SMTP (por ejemplo `smtp.example.com`, `587`) |
| `MAIL_STARTTLS`, `MAIL_SSL` | `true`/`false` para 587; `false`/`true` para 465 |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Casilla que envía los emails |
| `MAIL_FROM` | Remitente, por ejemplo `EsthetiAdmin <turnos@example.com>` |
| `PANEL_URL` | Dirección del panel, para el botón "Abrir la agenda" del email a la responsable |

En `prod` no hay valores por defecto: si falta una variable, la aplicación no arranca. Las de email son la excepción: sin ellas arranca con los emails apagados.

## Puesta en marcha (frontend)

Requisitos: Node y npm. Ver [frontend/README.md](frontend/README.md).

```bash
cd frontend
npm install
npm start
```

El panel queda en `http://localhost:4200` y la página pública de turnos en `http://localhost:4200/turnos`.

### Variables de entorno (frontend)

Están en `frontend/src/environments/` (`environment.development.ts` para desarrollo y `environment.ts` para producción):

| Variable | Descripción |
|---|---|
| `apiUrl` | URL base de la API (`.../api`) |
| `publicBookingUrl` | Link de la página pública de turnos que la Agenda muestra para copiar |

## Despliegue

Detalle y motivos en el [ADR 011](docs/adr/011-despliegue-produccion.md).

**Backend (VPS)**

1. Construir y subir la imagen desde `backend/`:
   ```bash
   docker build -t jonathanillescas/esthetiadmin-backend:1.2.0 -t jonathanillescas/esthetiadmin-backend:latest .
   docker push jonathanillescas/esthetiadmin-backend:1.2.0
   docker push jonathanillescas/esthetiadmin-backend:latest
   ```
2. En el VPS, en una carpeta con el `.env` de `prod` (ver variables abajo, `chmod 600`) y este `docker-compose.yml`:
   ```yaml
   services:
     esthetiadmin-backend:
       image: jonathanillescas/esthetiadmin-backend:1.2.0
       container_name: esthetiadmin-backend
       restart: unless-stopped
       network_mode: host
       env_file: .env
       mem_limit: 450m
       memswap_limit: 450m
   ```
   Además de las variables de la tabla, el `.env` de `prod` lleva `SERVER_ADDRESS=127.0.0.1` y `SERVER_PORT=8082`.
3. Levantar: `docker compose up -d`. Para actualizar, cambiar el tag y repetir el comando.
4. nginx hace de proxy hacia `127.0.0.1:8082` y certbot emite el certificado HTTPS.

**Frontend (hosting estático)**

1. Revisar `apiUrl` y `publicBookingUrl` en `frontend/src/environments/environment.ts` y compilar: `npm run build`.
2. Subir la carpeta `dist` a la raíz del subdominio y, junto a ella, `frontend/deploy/.htaccess` (una sola vez). El `.htaccess` hace que rutas como `/turnos` funcionen al recargar o al entrar directo desde el link.

Las migraciones de la base (Flyway) corren solas al arrancar el backend: no hay que ejecutar SQL a mano al actualizar. En `prod`, `server.forward-headers-strategy=native` toma la IP real de la clienta detrás de nginx, necesaria para los límites de uso (ADR 015).

## Documentación

- [Visión](docs/vision.md)
- [Modelo de datos](docs/data-model.md)
- [Contrato de la API](docs/api-contract.md) (el detalle por endpoint está en Swagger UI)
- [Stack y decisiones de arquitectura (ADR)](docs/stack.md)

## Estado

Proyecto personal de portfolio. Backend en `api.esthetiadmin.example.com`, frontend en `esthetiadmin.example.com` y página pública en `https://esthetiadmin.example.com/turnos` (dominios de ejemplo).
