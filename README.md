# 🏢 Coworking Booking Service

Microservicio REST para gestionar reservas de espacios de coworking.

## Qué hace

- CRUD de espacios (salas, escritorios, oficinas privadas).
- Registro y login con JWT. Hay roles ADMIN y USER, y los permisos de cada rol se pueden cambiar por API.
- Reservas sin solapamiento: un espacio no se puede reservar dos veces en el mismo horario. Cada usuario ve y gestiona las suyas, el admin ve todas.
- Confirmación con pago: se valida contra un proveedor externo (simulado con WireMock) protegido con circuit breaker.
- Al confirmar una reserva se manda una notificación en segundo plano.
- Reporte de ocupación por espacio y rango de fechas, cacheado.

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
| Circuit breakers | http://localhost:8080/coworking-service/actuator/circuitbreakers (solo ADMIN) |
| WireMock | http://localhost:8081 |
| PostgreSQL | `localhost:5433` (db, usuario y contraseña: `coworking`) |

La app corre con el perfil `prod` y construye su imagen desde el `Dockerfile` del repositorio.

Administrador inicial: `manuel.admin@coworking.com` / `Admin123!`. El token se obtiene con `POST /auth/login` y se envía como `Authorization: Bearer <token>`.

WireMock simula el proveedor de pagos según el importe de la reserva (`POST /reservations/{id}/confirm`):

| Importe | Respuesta del proveedor | Resultado |
|---------|-------------------------|-----------|
| cualquier otro | `APPROVED` | 200, reserva `CONFIRMED` |
| >= 1000 | `DECLINED` | 422, sigue `PENDING_PAYMENT` |
| 555 | 503 | 202, sigue `PENDING_PAYMENT` (cuenta como fallo) |
| 333 | tarda 3 s | 202 al superar el timeout de 2 s |

Si el proveedor falla 5 veces seguidas, el circuito se abre 30 s. Mientras está abierto, `confirm` responde 202 al instante sin llamar al proveedor. El estado se puede ver en `/actuator/circuitbreakers` y en `/actuator/health`.

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

Los tests levantan su propio PostgreSQL con Testcontainers, así que hace falta Docker encendido.

- Unitarios con Mockito para las reglas de negocio de espacios, usuarios, roles, reservas y el reporte.
- De integración (`*IT`), con la app completa, JWT real y PostgreSQL + WireMock en contenedores (usan los mismos mappings de `wiremock/`):
  - `PaymentConfirmationIT`: pago aprobado, rechazado, proveedor lento, el circuito abriéndose tras 5 fallos y la notificación asíncrona.
  - `ReservationConcurrencyIT`: 20 reservas al mismo tiempo para el mismo horario. Una sola gana (201), las otras 19 reciben 409 y en la base queda una fila. También revisa que dos reservas seguidas (9-10 y 10-11) enviadas a la vez pasen las dos.
  - `OccupancyReportIT`: el cálculo del reporte, que se cachea y que se actualiza al confirmar o cancelar.

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
│   ├── model/        # entidad JPA y estados (patrón State)
│   ├── dto/
│   ├── mapper/
│   ├── client/       # cliente del proveedor de pagos, con circuit breaker
│   └── event/        # eventos de dominio (ReservationConfirmedEvent)
├── notification/     # escucha eventos de reservas y notifica de forma asíncrona
├── report/           # reporte de ocupación cacheado
├── common/           # excepciones de negocio y @ControllerAdvice (transversal)
└── config/           # @ConfigurationProperties, seguridad, OpenAPI (transversal)
```

Cada dominio sigue el flujo `controller → service → repository`, sin saltarse capas.

```
src/main/resources
├── application.yml       # configuración base, se combina con el perfil activo
├── application-dev.yml   # datasource local, Flyway activado
└── application-prod.yml  # valores desde variables de entorno
```

## Decisiones y trade-offs

### Paquetes por dominio
Preferí agrupar por dominio (`space`, `user`, `reservation`...) y no por capa. Cada dominio tiene todo lo suyo junto y, si algún día hay que sacarlo a otro servicio, es más fácil. Lo malo es que las carpetas `controller/service/repository/...` se repiten en cada uno.

### Roles y permisos dinámicos
Los roles se crean y se editan por API (`/admin/roles`). Los permisos sí son un catálogo fijo, porque el código los revisa en cada endpoint con `@PreAuthorize`. El JWT solo dice quién es el usuario: sus permisos y si está bloqueado se leen de la base en cada petición (con caché). Así, si cambio el rol de alguien o lo bloqueo, aplica al momento sin esperar a que venza el token. Al rol ADMIN no se le pueden quitar `USER_MANAGE` ni `RBAC_MANAGE`, para no quedarse sin nadie que administre.

### Reservas que no se pisan
El servicio revisa el solapamiento antes de guardar para dar un error claro, pero eso solo no alcanza con dos peticiones al mismo tiempo. Por eso la regla también está en PostgreSQL con un `EXCLUDE USING gist` sobre `tstzrange(start_at, end_at)`: si dos llegan juntas, la base deja pasar una y la otra recibe 409. No usé locks en la aplicación porque la base ya lo resuelve y funciona igual con varias instancias. El rango es `[inicio, fin)`, así que una reserva de 9 a 10 y otra de 10 a 11 conviven sin problema.

### Patrón State
Cada estado de la reserva (`PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`) sabe a qué estados puede pasar. Si se intenta algo que no toca, como confirmar una cancelada, responde 409, y el servicio no se llena de `if`.

### Idempotency-Key
Crear una reserva exige la cabecera `Idempotency-Key`. Si el cliente reintenta con la misma clave (por un timeout, por ejemplo), recibe la reserva que ya se creó en vez de una duplicada.

### Pago con circuit breaker
La llamada al proveedor de pagos se hace fuera de la transacción, para no tener una conexión a la base ocupada mientras esperamos una respuesta que puede tardar. Solo cuando el pago se aprueba se abre una transacción corta para pasarla a `CONFIRMED`. Si el proveedor falla, tarda más de 2 s o el circuito está abierto, la reserva se queda en `PENDING_PAYMENT`, se responde 202 y se puede reintentar después. Un pago rechazado no es culpa del proveedor, así que ahí respondo 422 y no cuenta como fallo del circuito. El estado del circuito aparece en `/actuator/health`, pero no lo pone en DOWN: no quiero que Render reinicie la app porque un tercero esté caído.

### Notificación con eventos (Observer)
Al confirmar, el servicio publica un `ReservationConfirmedEvent` y se olvida. `ReservationNotificationListener` lo escucha con `@TransactionalEventListener(AFTER_COMMIT)` y `@Async`: solo notifica si la confirmación de verdad quedó guardada y no hace esperar al usuario. Por ahora la "notificación" es un log. Cambiarla por un email o una cola no obliga a tocar nada de reservas. El pool de hilos se configura en `spring.task.execution` y el correlation id se mantiene en los logs del hilo asíncrono. El punto débil: si la app se cae justo entre el commit y el envío, esa notificación se pierde. Para garantizarla haría falta un outbox.

### Reporte de ocupación con caché
`GET /reports/occupancy?from=...&to=...` (con `spaceId` opcional) saca en una sola consulta las horas reservadas de cada espacio dentro del rango, recortando las reservas que empiezan antes o terminan después, y las divide entre 24 h por día (UTC). Solo cuento las `CONFIRMED`, porque una pendiente puede no pagarse nunca. El resultado se guarda con `@Cacheable` y se limpia con `@CacheEvict` cuando se confirma o cancela una reserva o cambia un espacio. El cache manager es transaccional: la limpieza espera al commit, así un reporte que se pida justo en medio no vuelve a guardar datos viejos. La caché es en memoria; con varias instancias habría que pasarla a Redis.

### Swagger abierto en prod
Lo dejé accesible para que se pueda probar la API sin montar nada. En un entorno real lo cerraría desde la infraestructura (red interna o gateway), no desde el código.

### Credenciales
El admin inicial se crea al arrancar con `ADMIN_EMAIL` y `ADMIN_PASSWORD`, y `JWT_SECRET` firma los tokens. Los valores que aparecen en este README, en `.env.example` y en `docker-compose.yml` son solo para la evaluación. En producción irían en un gestor de secretos.

## Fuera de alcance

Dejé fuera a propósito:

- Notificaciones reales (email, SMS, colas) y el outbox para no perder ninguna.
- Un proveedor de pagos real. En Render no hay WireMock, así que ahí `confirm` siempre responde 202 y la reserva queda pendiente: es el fallback del circuit breaker, no un bug. El flujo completo del pago se ve con `docker compose up` o en los tests de integración.
- Reintentar solos los pagos pendientes o hacer vencer las reservas sin pagar. Hoy se reintenta llamando otra vez a `confirm`.
- Caché compartida entre instancias (Redis).
- Refresh tokens. El token dura 1 h, aunque bloquear a un usuario o cambiarle el rol aplica al instante.
- Horarios de apertura y zonas horarias por sede. El reporte asume 24 h en UTC.
- Reembolsos al cancelar una reserva ya pagada.
- Rate limiting; eso lo dejaría al gateway.
