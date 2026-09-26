package com.cowork.booking.common;

import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Messages.Common;
import com.cowork.booking.common.AppConstants.Messages.Validation;
import com.cowork.booking.common.AppConstants.Problem;
import com.cowork.booking.common.AppConstants.Tracing;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** RFC 9457 errors enriched with {@code code}, {@code timestamp} and {@code traceId}. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = problem(HttpStatus.NOT_FOUND, Common.NOT_FOUND_TITLE, ex.getMessage(), ErrorCodes.RESOURCE_NOT_FOUND);
        problem.setProperty(Problem.RESOURCE_TYPE, ex.getResourceType());
        problem.setProperty(Problem.RESOURCE_ID, ex.getResourceId());
        return problem;
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, Common.BUSINESS_RULE_TITLE, ex.getMessage(), ex.getCode());
        if (ex.getConflictingField() != null) {
            problem.setProperty(Problem.CONFLICTING_FIELD, ex.getConflictingField());
        }
        return problem;
    }

    // Concurrent requests that pass the service checks but hit a DB constraint.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn(Common.LOG_DATA_INTEGRITY, ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, Common.DATA_INTEGRITY_TITLE, Common.DATA_INTEGRITY_DETAIL,
                ErrorCodes.DATA_INTEGRITY_VIOLATION);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, Common.CONCURRENT_MODIFICATION_TITLE, Common.CONCURRENT_MODIFICATION_DETAIL,
                ErrorCodes.CONCURRENT_MODIFICATION);
    }

    // Unknown property in ?sort=... query parameter
    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail handleInvalidSort(PropertyReferenceException ex) {
        return problem(HttpStatus.BAD_REQUEST, Common.INVALID_SORT_TITLE,
                Common.INVALID_SORT_DETAIL.formatted(ex.getPropertyName()), ErrorCodes.INVALID_SORT);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error(Common.LOG_UNEXPECTED_ERROR, ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, Common.INTERNAL_ERROR_TITLE, Common.INTERNAL_ERROR_DETAIL,
                ErrorCodes.INTERNAL_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, Common.VALIDATION_FAILED_TITLE,
                Common.VALIDATION_FAILED_DETAIL, ErrorCodes.VALIDATION_ERROR);
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toViolation)
                .toList();
        body.setProperty(Problem.ERRORS, errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, Common.MALFORMED_REQUEST_TITLE,
                Common.MALFORMED_REQUEST_DETAIL, ErrorCodes.MALFORMED_REQUEST);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, Common.INVALID_PARAMETER_TITLE,
                Common.INVALID_PARAMETER_DETAIL.formatted(ex.getPropertyName()), ErrorCodes.INVALID_PARAMETER);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    // Spring MVC errors (404, 405, 415...): same contract, our messages.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem && !hasCode(problem)) {
            FrameworkError error = FrameworkError.of(statusCode);
            problem.setTitle(error.title());
            problem.setDetail(error.detail());
            enrich(problem, error.code());
        }
        return response;
    }

    private record FrameworkError(String code, String title, String detail) {

        static FrameworkError of(HttpStatusCode status) {
            return switch (status.value()) {
                case 404 -> new FrameworkError(ErrorCodes.ROUTE_NOT_FOUND, Common.NOT_FOUND_TITLE, Common.ROUTE_NOT_FOUND_DETAIL);
                case 405 -> new FrameworkError(ErrorCodes.METHOD_NOT_ALLOWED, Common.METHOD_NOT_ALLOWED_TITLE,
                        Common.METHOD_NOT_ALLOWED_DETAIL);
                case 415 -> new FrameworkError(ErrorCodes.UNSUPPORTED_MEDIA_TYPE, Common.UNSUPPORTED_MEDIA_TYPE_TITLE,
                        Common.UNSUPPORTED_MEDIA_TYPE_DETAIL);
                default -> status.is5xxServerError()
                        ? new FrameworkError(ErrorCodes.INTERNAL_ERROR, Common.INTERNAL_ERROR_TITLE, Common.INTERNAL_ERROR_DETAIL)
                        : new FrameworkError(ErrorCodes.REQUEST_ERROR, Common.REQUEST_ERROR_TITLE, Common.REQUEST_ERROR_DETAIL);
            };
        }
    }

    private static ProblemDetail problem(HttpStatus status, String title, @Nullable String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        enrich(problem, code);
        return problem;
    }

    private static void enrich(ProblemDetail problem, String code) {
        problem.setType(problemType(code));
        problem.setProperty(Problem.CODE, code);
        problem.setProperty(Problem.TIMESTAMP, Instant.now());
        String traceId = MDC.get(Tracing.MDC_KEY);
        if (traceId != null) {
            problem.setProperty(Problem.TRACE_ID, traceId);
        }
    }

    // Per-environment URL; proxy-aware via server.forward-headers-strategy.
    private static URI problemType(String code) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(Problem.ERRORS_PATH + Problem.slug(code))
                .build()
                .toUri();
    }

    private static boolean hasCode(ProblemDetail problem) {
        return problem.getProperties() != null && problem.getProperties().containsKey(Problem.CODE);
    }

    private static Map<String, String> toViolation(FieldError error) {
        String message = error.isBindingFailure() ? Validation.INVALID_VALUE : String.valueOf(error.getDefaultMessage());
        return Map.of(
                Problem.VIOLATION_FIELD, error.getField(),
                Problem.VIOLATION_CODE, toConstraintCode(error.getCode()),
                Problem.VIOLATION_MESSAGE, message);
    }

    // "NotBlank" -> "NOT_BLANK", "typeMismatch" -> "TYPE_MISMATCH"
    private static String toConstraintCode(@Nullable String constraint) {
        if (constraint == null) {
            return ErrorCodes.VALIDATION_ERROR;
        }
        return constraint.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }
}
