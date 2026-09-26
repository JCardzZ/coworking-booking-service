package com.cowork.booking.common;

import java.util.List;
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

        private Api() {
        }
    }

    public static final class Paging {
        public static final int DEFAULT_SIZE = 20;
        public static final String SPACE_DEFAULT_SORT = "name";

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

        public static final List<String> PUBLIC_PATHS = List.of(
                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health/**",
                // TODO(jwt): remove
                Api.BASE_PATH + Api.SPACES + "/**");

        private Security() {
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
        public static final String DATA_INTEGRITY_VIOLATION = "DATA_INTEGRITY_VIOLATION";
        public static final String CONCURRENT_MODIFICATION = "CONCURRENT_MODIFICATION";
        public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

        public static final String SPACE_NAME_TAKEN = "SPACE_NAME_TAKEN";

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
            public static final String REQUEST_ERROR_DETAIL = "No se pudo procesar la solicitud";
            public static final String BUSINESS_RULE_TITLE = "Regla de negocio incumplida";
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
    }

    /** Must match the Flyway column definitions. */
    public static final class Limits {
        public static final int ENUM_MAX = 30;
        public static final int SPACE_NAME_MAX = 100;
        public static final int SPACE_LOCATION_MAX = 150;
        public static final int MONEY_INTEGER_DIGITS = 8;
        public static final int MONEY_FRACTION_DIGITS = 2;

        private Limits() {
        }
    }
}
