# 🏢 Coworking Booking Service

Microservicio REST para gestionar reservas de espacios de coworking.

## Alcance funcional

- **Espacios**: alta, consulta y administración de los espacios reservables.
- **Usuarios con roles**: usuarios autenticados con JWT y permisos según su rol.
- **Reservas sin solapamiento**: un espacio no puede tener dos reservas que se crucen en el tiempo.
- **Notificación asíncrona**: al confirmar una reserva se notifica sin bloquear la respuesta.
- **Reporte de ocupación cacheado**: métricas de ocupación por espacio servidas desde caché.
- **Validación de pago externa**: llamada a un servicio de pagos protegida con circuit breaker (Resilience4j). El proveedor de pagos se simula con WireMock (`PAYMENT_SERVICE_URL`).

## Stack técnico

| Área | Tecnología |
|------|------------|
| Lenguaje / framework | Java 21, Spring Boot 3.5 |
| Persistencia | PostgreSQL 17, Spring Data JPA, Flyway |
| Seguridad | Spring Security, OAuth2 Resource Server (JWT) |
| Resiliencia | Resilience4j (Spring Cloud Circuit Breaker) |
| Documentación API | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5, Testcontainers |
| Empaquetado / CI | Docker (multi-arch), GitHub Actions, GHCR |

## Cómo arrancarlo

Requisitos: Docker. Para el flujo de desarrollo, además JDK 21.

### Stack completo (PostgreSQL + WireMock + app)

```bash
docker compose up
```

| Servicio | URL |
|----------|-----|
| API | http://localhost:8080/coworking-service/api/v1 |
| Swagger UI | http://localhost:8080/coworking-service/swagger-ui.html |
| OpenAPI (JSON) | http://localhost:8080/coworking-service/v3/api-docs |
| Health | http://localhost:8080/coworking-service/actuator/health (detalle solo ADMIN) |
| Info | http://localhost:8080/coworking-service/actuator/info |
| Métricas | http://localhost:8080/coworking-service/actuator/metrics (solo ADMIN) |
| WireMock | http://localhost:8081 |
| PostgreSQL | `localhost:5433` (db, usuario y contraseña: `coworking`) |

La app corre con el perfil `prod` y construye su imagen desde el `Dockerfile` del repositorio.

Administrador inicial: `manuel.admin@coworking.com` / `Admin123!`. El token se obtiene con `POST /auth/login` y se envía como `Authorization: Bearer <token>`.

### Desarrollo local

```bash
cp .env.example .env                              # una sola vez
docker compose up -d postgres                     # solo la base de datos, en localhost:5433
set -a; source .env; set +a && ./gradlew bootRun  # perfil "dev" por defecto
```

Hook de git (una sola vez): `git config core.hooksPath .githooks`. Bloquea el `git push` si `./gradlew build` falla. Requiere Docker encendido (los tests usan Testcontainers); en una emergencia se omite con `git push --no-verify`.

`application-dev.yml` no tiene valores por defecto: las variables de `.env` son obligatorias. Spring no lee `.env` por sí solo; en IntelliJ se carga con el plugin **EnvFile** (Settings → Plugins → buscar "EnvFile"), activándolo en Run/Debug Configurations → pestaña EnvFile → agregar `.env`.

### Tests

```bash
./gradlew build
```

Los tests levantan su propio PostgreSQL efímero con Testcontainers (requiere Docker).

## Arquitectura

```
com.cowork.booking
├── space/            # dominio: espacios reservables
│   ├── controller/   # entrada HTTP, valida y delega
│   ├── service/      # lógica de negocio, @Transactional
│   ├── repository/   # acceso a datos, Spring Data JPA
│   ├── model/        # entidad JPA (@Entity)
│   ├── dto/          # contrato de la API, nunca expone entidades JPA
│   └── mapper/       # entidad (model/) <-> DTO
├── user/             # dominio: usuarios y roles
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/        # entidad JPA (@Entity)
│   ├── dto/
│   └── mapper/
├── reservation/      # dominio: reservas (regla de no solapamiento vive aquí)
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/        # entidad JPA (@Entity)
│   ├── dto/
│   └── mapper/
├── common/           # excepciones de negocio y @ControllerAdvice (transversal)
└── config/           # @ConfigurationProperties, seguridad, OpenAPI (transversal)
```

Organización por dominio (package by feature): cada dominio mantiene el flujo `controller → service → repository` sin saltos.

```
src/main/resources
├── application.yml       # configuración base, se combina con el perfil activo
├── application-dev.yml   # datasource local, Flyway activado
└── application-prod.yml  # valores desde variables de entorno
```

## Decisiones de diseño y trade-offs

- **Swagger UI accesible también con el perfil `prod`**: se deja expuesto para que el evaluador pueda explorar y probar la API con `docker compose up`. En un despliegue real se restringiría a nivel de infraestructura (red interna o API Gateway), no desactivándolo desde el código de la aplicación.
- **Paquetes por dominio en vez de por capa**: se gana cohesión (cada dominio agrupa todo lo que necesita), dominios aislados entre sí y más fáciles de extraer a otro servicio si hiciera falta. A cambio, las subcarpetas `controller/service/repository/dto/mapper` se repiten en cada dominio.
- **Credenciales de evaluación**: el admin inicial se crea al arrancar desde `ADMIN_EMAIL`/`ADMIN_PASSWORD`, y `JWT_SECRET` firma los tokens. Los valores del README, `.env.example` y `docker-compose.yml` son solo para evaluación; en un despliegue real se sustituyen por secretos gestionados (vault o secretos del orquestador).

_Resto pendiente de completar._

## Fuera de alcance

_Pendiente de completar._
