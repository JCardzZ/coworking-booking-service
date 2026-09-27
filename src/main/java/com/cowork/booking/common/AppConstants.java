package com.cowork.booking.common;

import java.util.List;
import java.util.Set;
import java.util.Locale;

/** Runtime constants grouped by flow. */
public final class AppConstants {

    private AppConstants() {
    }

    public static final class Api {
        /** Added by {@code WebConfig}. */
        public static final String BASE_PATH = "/api/v1";

        public static final String SPACES = "/spaces";
        public static final String SPACE_ID = "/{spaceId}";

        public static final String AUTH = "/auth";
        public static final String REGISTER = "/register";
        public static final String LOGIN = "/login";

        public static final String USERS = "/users";
        public static final String ME = "/me";

        public static final String ADMIN_USERS = "/admin/users";
        public static final String ADMIN_ROLES = "/admin/roles";
        public static final String ADMIN_PERMISSIONS = "/admin/permissions";
        public static final String USER_ID = "/{userId}";
        public static final String ROLE_ID = "/{roleId}";
        public static final String STATUS = "/status";
        public static final String PERMISSIONS = "/permissions";

        public static final String RESERVATIONS = "/reservations";
        public static final String RESERVATION_ID = "/{reservationId}";
        public static final String CANCEL = "/cancel";
        public static final String CONFIRM = "/confirm";

        public static final String REPORTS = "/reports";
        public static final String OCCUPANCY = "/occupancy";

        public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

        private Api() {
        }
    }

    public static final class Paging {
        public static final int DEFAULT_SIZE = 20;
        public static final String SPACE_DEFAULT_SORT = "name";
        public static final String USER_DEFAULT_SORT = "email";
        public static final String RESERVATION_DEFAULT_SORT = "startAt";

        private Paging() {
        }
    }

    public static final class Tracing {
        public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
        public static final String MDC_KEY = "correlationId";
        public static final String VALID_ID_REGEX = "[A-Za-z0-9-]{1,64}";

        private Tracing() {
        }
    }

    public static final class Security {
        public static final String BEARER_SCHEME = "bearerAuth";
        public static final String TOKEN_TYPE = "Bearer";
        public static final String ISSUER = "coworking-service";
        public static final String ROLE_CLAIM = "role";
        public static final String EMAIL_CLAIM = "email";
        public static final String ROLE_PREFIX = "ROLE_";
        public static final String ADMIN_ROLE = "ADMIN";
        public static final String DEFAULT_ROLE = "USER";

        public static final List<String> PUBLIC_PATHS = List.of(
                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health/**", "/actuator/info",
                Api.BASE_PATH + Api.AUTH + "/**");

        /** Operational data: only for administrators. */
        public static final List<String> ADMIN_PATHS = List.of("/actuator/metrics/**", "/actuator/circuitbreakers/**");

        private Security() {
        }
    }

    /** Permission catalog (seeded by Flyway V4) and the expressions used in @PreAuthorize. */
    public static final class Permissions {
        public static final String SPACE_READ = "SPACE_READ";
        public static final String SPACE_WRITE = "SPACE_WRITE";
        public static final String USER_MANAGE = "USER_MANAGE";
        public static final String RBAC_MANAGE = "RBAC_MANAGE";
        public static final String METRICS_READ = "METRICS_READ";
        public static final String RESERVATION_CREATE = "RESERVATION_CREATE";
        public static final String RESERVATION_READ_OWN = "RESERVATION_READ_OWN";
        public static final String RESERVATION_READ_ALL = "RESERVATION_READ_ALL";
        public static final String RESERVATION_MANAGE_ALL = "RESERVATION_MANAGE_ALL";
        public static final String REPORT_READ = "REPORT_READ";

        public static final String HAS_SPACE_READ = "hasAuthority('" + SPACE_READ + "')";
        public static final String HAS_SPACE_WRITE = "hasAuthority('" + SPACE_WRITE + "')";
        public static final String HAS_USER_MANAGE = "hasAuthority('" + USER_MANAGE + "')";
        public static final String HAS_RBAC_MANAGE = "hasAuthority('" + RBAC_MANAGE + "')";
        public static final String HAS_RESERVATION_CREATE = "hasAuthority('" + RESERVATION_CREATE + "')";
        public static final String HAS_REPORT_READ = "hasAuthority('" + REPORT_READ + "')";
        public static final String CAN_READ_RESERVATIONS =
                "hasAnyAuthority('" + RESERVATION_READ_OWN + "', '" + RESERVATION_READ_ALL + "')";
        public static final String CAN_CONFIRM_RESERVATIONS =
                "hasAnyAuthority('" + RESERVATION_CREATE + "', '" + RESERVATION_MANAGE_ALL + "')";
        public static final String CAN_CANCEL_RESERVATIONS =
                "hasAnyAuthority('" + RESERVATION_CREATE + "', '" + RESERVATION_MANAGE_ALL + "')";

        /** ADMIN always keeps these so nobody gets locked out. */
        public static final Set<String> ADMIN_LOCKED = Set.of(USER_MANAGE, RBAC_MANAGE);

        private Permissions() {
        }
    }

    public static final class Caches {
        public static final String USER_AUTHORIZATION = "user-authorization";
        public static final String OCCUPANCY_REPORT = "occupancy-report";

        private Caches() {
        }
    }

    public static final class Audit {
        public static final String LOGGER = "AUDIT";
        public static final String SYSTEM_USER = "system";
        public static final String LOG_SUCCESS = "action={} user={} role={} outcome=SUCCESS";
        public static final String LOG_FAILURE = "action={} user={} role={} outcome=FAILURE reason={}";

        public static final String SPACE_CREATE = "SPACE_CREATE";
        public static final String SPACE_UPDATE = "SPACE_UPDATE";
        public static final String SPACE_DELETE = "SPACE_DELETE";
        public static final String RESERVATION_CREATE = "RESERVATION_CREATE";
        public static final String RESERVATION_CANCEL = "RESERVATION_CANCEL";
        public static final String RESERVATION_CONFIRM = "RESERVATION_CONFIRM";
        public static final String USER_REGISTER = "USER_REGISTER";
        public static final String USER_CREATE = "USER_CREATE";
        public static final String USER_STATUS_UPDATE = "USER_STATUS_UPDATE";
        public static final String ROLE_CREATE = "ROLE_CREATE";
        public static final String ROLE_PERMISSIONS_UPDATE = "ROLE_PERMISSIONS_UPDATE";

        private Audit() {
        }
    }

    /** Values of the {@code code} error field. */
    public static final class ErrorCodes {
        public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
        public static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
        public static final String INVALID_PARAMETER = "INVALID_PARAMETER";
        public static final String INVALID_SORT = "INVALID_SORT";
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String FORBIDDEN = "FORBIDDEN";
        public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
        public static final String ROUTE_NOT_FOUND = "ROUTE_NOT_FOUND";
        public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
        public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
        public static final String REQUEST_ERROR = "REQUEST_ERROR";
        public static final String MISSING_HEADER = "MISSING_HEADER";
        public static final String DATA_INTEGRITY_VIOLATION = "DATA_INTEGRITY_VIOLATION";
        public static final String CONCURRENT_MODIFICATION = "CONCURRENT_MODIFICATION";
        public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

        public static final String SPACE_NAME_TAKEN = "SPACE_NAME_TAKEN";
        public static final String EMAIL_TAKEN = "EMAIL_TAKEN";
        public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
        public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
        public static final String ROLE_NAME_TAKEN = "ROLE_NAME_TAKEN";
        public static final String UNKNOWN_ROLE = "UNKNOWN_ROLE";
        public static final String UNKNOWN_PERMISSION = "UNKNOWN_PERMISSION";
        public static final String ADMIN_ROLE_LOCKOUT = "ADMIN_ROLE_LOCKOUT";
        public static final String SELF_STATUS_CHANGE = "SELF_STATUS_CHANGE";
        public static final String RESERVATION_OVERLAP = "RESERVATION_OVERLAP";
        public static final String RESERVATION_IN_PAST = "RESERVATION_IN_PAST";
        public static final String RESERVATION_TOO_LONG = "RESERVATION_TOO_LONG";
        public static final String RESERVATION_ALREADY_STARTED = "RESERVATION_ALREADY_STARTED";
        public static final String INVALID_RESERVATION_STATE = "INVALID_RESERVATION_STATE";
        public static final String IDEMPOTENCY_KEY_REUSED = "IDEMPOTENCY_KEY_REUSED";
        public static final String PAYMENT_DECLINED = "PAYMENT_DECLINED";

        private ErrorCodes() {
        }
    }

    /** ProblemDetail members and extensions. */
    public static final class Problem {
        public static final String TYPE = "type";
        public static final String TITLE = "title";
        public static final String STATUS = "status";
        public static final String DETAIL = "detail";
        public static final String INSTANCE = "instance";
        public static final String CODE = "code";
        public static final String TIMESTAMP = "timestamp";
        public static final String TRACE_ID = "traceId";
        public static final String ERRORS = "errors";
        public static final String RESOURCE_TYPE = "resourceType";
        public static final String RESOURCE_ID = "resourceId";
        public static final String CONFLICTING_FIELD = "conflictingField";

        public static final String VIOLATION_FIELD = "field";
        public static final String VIOLATION_CODE = "code";
        public static final String VIOLATION_MESSAGE = "message";

        public static final String ERRORS_PATH = "/errors/";

        private Problem() {
        }

        /** {@code SPACE_NAME_TAKEN} -> {@code space-name-taken} */
        public static String slug(String code) {
            return code.toLowerCase(Locale.ROOT).replace('_', '-');
        }

    }

    public static final class Messages {

        private Messages() {
        }

        public static final class Common {
            public static final String VALIDATION_FAILED_TITLE = "Error de validación";
            public static final String VALIDATION_FAILED_DETAIL = "Uno o más campos no son válidos";
            public static final String MALFORMED_REQUEST_TITLE = "Solicitud mal formada";
            public static final String MALFORMED_REQUEST_DETAIL = "El cuerpo de la solicitud no es un JSON válido o contiene valores no permitidos";
            public static final String INVALID_PARAMETER_TITLE = "Parámetro inválido";
            public static final String INVALID_PARAMETER_DETAIL = "El parámetro '%s' tiene un valor inválido";
            public static final String INVALID_SORT_TITLE = "Ordenamiento inválido";
            public static final String INVALID_SORT_DETAIL = "No se puede ordenar por la propiedad '%s'";
            public static final String UNAUTHORIZED_TITLE = "No autorizado";
            public static final String UNAUTHORIZED_DETAIL = "Token de acceso ausente, inválido o expirado";
            public static final String FORBIDDEN_TITLE = "Acceso denegado";
            public static final String FORBIDDEN_DETAIL = "No tienes permisos para realizar esta operación";
            public static final String NOT_FOUND_TITLE = "Recurso no encontrado";
            public static final String ROUTE_NOT_FOUND_DETAIL = "La ruta solicitada no existe";
            public static final String METHOD_NOT_ALLOWED_TITLE = "Método no permitido";
            public static final String METHOD_NOT_ALLOWED_DETAIL = "El método HTTP no está permitido para este recurso";
            public static final String UNSUPPORTED_MEDIA_TYPE_TITLE = "Tipo de contenido no soportado";
            public static final String UNSUPPORTED_MEDIA_TYPE_DETAIL = "El Content-Type de la solicitud no está soportado";
            public static final String REQUEST_ERROR_TITLE = "Solicitud no procesable";
            public static final String MISSING_HEADER_TITLE = "Cabecera obligatoria";
            public static final String MISSING_HEADER_DETAIL = "Falta la cabecera '%s'";
            public static final String REQUEST_ERROR_DETAIL = "No se pudo procesar la solicitud";
            public static final String BUSINESS_RULE_TITLE = "Regla de negocio incumplida";
            public static final String UNPROCESSABLE_TITLE = "Operación no permitida";
            public static final String DATA_INTEGRITY_TITLE = "Conflicto de datos";
            public static final String DATA_INTEGRITY_DETAIL = "La solicitud entra en conflicto con el estado actual de los datos";
            public static final String CONCURRENT_MODIFICATION_TITLE = "Modificación concurrente";
            public static final String CONCURRENT_MODIFICATION_DETAIL = "El recurso fue modificado por otra solicitud; vuelve a consultarlo e inténtalo de nuevo";
            public static final String INTERNAL_ERROR_TITLE = "Error interno del servidor";
            public static final String INTERNAL_ERROR_DETAIL = "Ocurrió un error inesperado";

            public static final String LOG_UNEXPECTED_ERROR = "Error inesperado";
            public static final String LOG_DATA_INTEGRITY = "Violación de integridad de datos: {}";

            private Common() {
            }
        }

        /** {placeholders} are interpolated by Bean Validation. */
        public static final class Validation {
            public static final String REQUIRED = "es obligatorio";
            public static final String NOT_BLANK = "no debe estar vacío";
            public static final String POSITIVE = "debe ser mayor que 0";
            public static final String MAX_LENGTH = "debe tener como máximo {max} caracteres";
            public static final String DECIMAL_FORMAT = "debe tener como máximo {integer} dígitos enteros y {fraction} decimales";
            public static final String INVALID_VALUE = "tiene un valor inválido";
            public static final String EMAIL_FORMAT = "debe ser un email válido";
            public static final String LENGTH_RANGE = "debe tener entre {min} y {max} caracteres";
            public static final String NOT_EMPTY = "debe tener al menos un elemento";
            public static final String END_AFTER_START = "endAt debe ser posterior a startAt";
            public static final String IDEMPOTENCY_KEY_FORMAT = "debe tener entre 8 y 64 caracteres: letras, números y guiones";
            public static final String ROLE_NAME_FORMAT = "debe empezar por letra y contener solo letras, números y guiones bajos";
        public static final String REPORT_RANGE = "to debe ser igual o posterior a from y el rango no puede superar 366 días";

            private Validation() {
            }
        }

        public static final class Space {
            public static final String RESOURCE_TYPE = "Space";
            public static final String NAME_FIELD = "name";
            public static final String NOT_FOUND = "Espacio con id %s no encontrado";
            public static final String NAME_TAKEN = "Ya existe un espacio con el nombre '%s'";

            private Space() {
            }
        }

        public static final class User {
            public static final String RESOURCE_TYPE = "User";
            public static final String NOT_FOUND = "Usuario con id %s no encontrado";

            private User() {
            }
        }

        public static final class Role {
            public static final String RESOURCE_TYPE = "Role";
            public static final String NAME_FIELD = "name";
            public static final String NOT_FOUND = "Rol con id %s no encontrado";
            public static final String NAME_TAKEN = "Ya existe un rol con el nombre '%s'";
            public static final String UNKNOWN = "El rol '%s' no existe";
            public static final String UNKNOWN_PERMISSIONS = "Permisos inexistentes: %s";
            public static final String ADMIN_LOCKOUT = "El rol ADMIN debe conservar los permisos %s";

            private Role() {
            }
        }

        public static final class Reservation {
            public static final String RESOURCE_TYPE = "Reservation";
            public static final String NOT_FOUND = "Reserva con id %s no encontrada";
            public static final String OVERLAP = "El espacio ya está reservado en ese horario";
            public static final String IN_PAST = "La reserva debe empezar en el futuro";
            public static final String TOO_LONG = "La reserva no puede durar más de %d horas";
            public static final String ALREADY_STARTED = "No se puede cancelar una reserva que ya comenzó";
            public static final String INVALID_TRANSITION = "No se puede %s una reserva en estado %s";
            public static final String IDEMPOTENCY_KEY_REUSED = "La Idempotency-Key ya se usó con otros datos de reserva";
            public static final String ACTION_CONFIRM = "confirmar";
            public static final String ACTION_CANCEL = "cancelar";
            public static final String PAYMENT_DECLINED = "El pago fue rechazado; la reserva sigue pendiente de pago";
            public static final String LOG_PAYMENT_UNAVAILABLE = "Payment service unavailable for reservation {}: {}";
            public static final String PERIOD_FIELD = "endAt";

            private Reservation() {
            }
        }

        public static final class Notification {
            public static final String LOG_RESERVATION_CONFIRMED =
                    "Notificación enviada a {}: reserva {} confirmada en '{}' de {} a {}, importe {}";

            private Notification() {
            }
        }

        public static final class Admin {
            public static final String SELF_STATUS_CHANGE = "Un administrador no puede cambiar el estado de su propia cuenta";

            private Admin() {
            }
        }

        public static final class Auth {
            public static final String EMAIL_FIELD = "email";
            public static final String EMAIL_TAKEN = "Ya existe un usuario con el email '%s'";
            public static final String INVALID_CREDENTIALS_TITLE = "Credenciales inválidas";
            public static final String INVALID_CREDENTIALS_DETAIL = "Email o contraseña incorrectos";
            public static final String LOG_ADMIN_CREATED = "Usuario administrador inicial creado: {}";
            public static final String ACCOUNT_DISABLED = "La cuenta está bloqueada";
            public static final String TOKEN_USER_INACTIVE = "El usuario del token no existe o está bloqueado";

            private Auth() {
            }
        }
    }

    /** Must match the Flyway column definitions. */
    public static final class Limits {
        public static final int ENUM_MAX = 30;
        public static final int SPACE_NAME_MAX = 100;
        public static final int SPACE_LOCATION_MAX = 150;
        public static final int MONEY_INTEGER_DIGITS = 8;
        public static final int MONEY_FRACTION_DIGITS = 2;
        public static final int EMAIL_MAX = 150;
        public static final int FULL_NAME_MAX = 100;
        /** BCrypt only uses the first 72 bytes. */
        public static final int PASSWORD_MIN = 8;
        public static final int PASSWORD_MAX = 72;
        public static final int AUDITOR_MAX = 150;
        public static final int ROLE_NAME_MAX = 50;
        public static final int DESCRIPTION_MAX = 200;
        /** Role names: letter first, then letters, digits or underscores; stored uppercase. */
        public static final String ROLE_NAME_PATTERN = "\\s*[A-Za-z][A-Za-z0-9_]{1,49}\\s*";
        public static final int RESERVATION_MAX_HOURS = 12;
        public static final int IDEMPOTENCY_KEY_MAX = 64;
        public static final String IDEMPOTENCY_KEY_PATTERN = "[A-Za-z0-9-]{8,64}";
        public static final int REPORT_MAX_DAYS = 366;

        private Limits() {
        }
    }
}
