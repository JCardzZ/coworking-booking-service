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

Con 5 fallos seguidos el circuito se abre durante 30 s: las confirmaciones responden 202 al instante sin llamar al proveedor, y su estado se ve en `/actuator/circuitbreakers` y en `/actuator/health`.

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

- **Unitarios** (Mockito): reglas de negocio de espacios, usuarios, roles y reservas.
- **Integración** (`*IT`, `@SpringBootTest` + Testcontainers): `PaymentConfirmationIT` levanta PostgreSQL y WireMock en contenedores (con los mismos mappings de `wiremock/`) y recorre la confirmación por HTTP con JWT real: pago aprobado, rechazado, proveedor lento, el circuito que se abre tras 5 fallos y la notificación asíncrona tras confirmar. `OccupancyReportIT` comprueba el cálculo del reporte, que se cachea y que se refresca al confirmar o cancelar. `ReservationConcurrencyIT` lanza 20 reservas simultáneas del mismo horario: solo una responde 201, las otras 19 reciben 409 y en la base queda una sola fila; además, dos reservas contiguas lanzadas a la vez se aceptan las dos.

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
- **RBAC dinámico**: los roles se crean y editan por API (`/admin/roles`); los permisos son un catálogo fijo porque el código los comprueba (`@PreAuthorize("hasAuthority('SPACE_WRITE')")`). El JWT solo identifica al usuario: sus permisos y su estado se leen de la base en cada petición (con caché que se invalida al cambiar un rol o bloquear una cuenta), así los cambios aplican al instante sin esperar a que caduque el token. El rol ADMIN no puede perder `USER_MANAGE` ni `RBAC_MANAGE`, para que nadie quede fuera de la administración.
- **Reservas sin solapamiento**: la regla se comprueba en el servicio (error claro) y además en PostgreSQL con una restricción `EXCLUDE USING gist` sobre `tstzrange(start_at, end_at)`, que impide dobles reservas incluso con peticiones simultáneas. Los rangos son semiabiertos `[inicio, fin)`, así que se permiten reservas contiguas.
- **Patrón State (GoF)**: cada estado de la reserva (`PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`) decide qué transiciones permite; una transición inválida responde 409 sin `if/else` repartidos por el servicio.
- **Idempotency-Key**: obligatoria al crear reservas; reenviar la misma clave devuelve la reserva original en vez de duplicarla.
- **Pago con circuit breaker (Resilience4j)**: la confirmación llama al proveedor fuera de cualquier transacción, para no tener una conexión a la base ocupada mientras se espera la respuesta, y solo abre una transacción corta para marcarla `CONFIRMED`. Si el proveedor falla, tarda más de 2 s o el circuito está abierto, el fallback deja la reserva en `PENDING_PAYMENT` y responde 202 para reintentarla después; un rechazo del pago no es un fallo del proveedor y responde 422. El indicador del circuito se muestra en `/actuator/health`, pero no lo pone en DOWN: un proveedor externo caído no debe hacer que la plataforma reinicie la app.
- **Notificación asíncrona con eventos de dominio (Observer)**: al confirmar, el servicio publica `ReservationConfirmedEvent` y no sabe quién lo escucha. `ReservationNotificationListener` lo recibe con `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`: solo notifica si la confirmación quedó guardada y no retrasa la respuesta. El envío es un log (mock); cambiarlo por email o una cola no toca el código de reservas. El pool se configura en `spring.task.execution` y conserva el correlation id en los logs del hilo asíncrono. Trade-off: si la app cae entre el commit y el envío, la notificación se pierde; para garantizarla haría falta un outbox transaccional.
- **Reporte de ocupación con caché**: `GET /reports/occupancy?from&to[&spaceId]` calcula en una sola consulta SQL, para todos los espacios, las horas de reservas `CONFIRMED` dentro del rango (recortando las que cruzan los bordes) sobre 24 h por día en UTC. Solo cuentan las confirmadas: una pendiente de pago puede no pagarse nunca. El resultado se guarda con `@Cacheable` y se invalida con `@CacheEvict` al confirmar o cancelar una reserva y al modificar un espacio. El cache manager es transaccional (`TransactionAwareCacheManagerProxy`), así la invalidación ocurre tras el commit y un reporte leído justo antes no vuelve a cachear datos viejos. Trade-off: la caché es en memoria de cada instancia; con varias réplicas haría falta Redis o similar.
- **Credenciales de evaluación**: el admin inicial se crea al arrancar desde `ADMIN_EMAIL`/`ADMIN_PASSWORD`, y `JWT_SECRET` firma los tokens. Los valores del README, `.env.example` y `docker-compose.yml` son solo para evaluación; en un despliegue real se sustituyen por secretos gestionados (vault o secretos del orquestador).

_Resto pendiente de completar._

## Fuera de alcance

_Pendiente de completar._
