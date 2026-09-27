package com.cowork.booking.common;

import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Messages;
import com.cowork.booking.common.AppConstants.Messages.Common;
import com.cowork.booking.common.AppConstants.Messages.Validation;
import com.cowork.booking.common.AppConstants.Problem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** RFC 9457 errors enriched with {@code code}, {@code timestamp} and {@code traceId}. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetails.of(HttpStatus.NOT_FOUND, Common.NOT_FOUND_TITLE, ex.getMessage(), ErrorCodes.RESOURCE_NOT_FOUND);
        problem.setProperty(Problem.RESOURCE_TYPE, ex.getResourceType());
        problem.setProperty(Problem.RESOURCE_ID, ex.getResourceId());
        return problem;
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        ProblemDetail problem = ProblemDetails.of(HttpStatus.CONFLICT, Common.BUSINESS_RULE_TITLE, ex.getMessage(), ex.getCode());
        if (ex.getConflictingField() != null) {
            problem.setProperty(Problem.CONFLICTING_FIELD, ex.getConflictingField());
        }
        return problem;
    }

    // DB constraint hit by concurrent requests that passed the service check
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn(Common.LOG_DATA_INTEGRITY, ex.getMostSpecificCause().getMessage());
        return ProblemDetails.of(HttpStatus.CONFLICT, Common.DATA_INTEGRITY_TITLE, Common.DATA_INTEGRITY_DETAIL,
                ErrorCodes.DATA_INTEGRITY_VIOLATION);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, Common.CONCURRENT_MODIFICATION_TITLE, Common.CONCURRENT_MODIFICATION_DETAIL,
                ErrorCodes.CONCURRENT_MODIFICATION);
    }

    // Unknown property in ?sort=... query parameter
    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail handleInvalidSort(PropertyReferenceException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, Common.INVALID_SORT_TITLE,
                Common.INVALID_SORT_DETAIL.formatted(ex.getPropertyName()), ErrorCodes.INVALID_SORT);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ProblemDetail handleAuthenticationFailed(AuthenticationFailedException ex) {
        return ProblemDetails.of(HttpStatus.UNAUTHORIZED, Messages.Auth.INVALID_CREDENTIALS_TITLE, ex.getMessage(),
                ex.getCode());
    }

    @ExceptionHandler(UnprocessableOperationException.class)
    ProblemDetail handleUnprocessable(UnprocessableOperationException ex) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, Common.UNPROCESSABLE_TITLE, ex.getMessage(), ex.getCode());
    }

    // @PreAuthorize denials are thrown inside MVC, so they land here
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return ProblemDetails.of(HttpStatus.FORBIDDEN, Common.FORBIDDEN_TITLE, Common.FORBIDDEN_DETAIL, ErrorCodes.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error(Common.LOG_UNEXPECTED_ERROR, ex);
        return ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, Common.INTERNAL_ERROR_TITLE, Common.INTERNAL_ERROR_DETAIL,
                ErrorCodes.INTERNAL_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ProblemDetails.of(HttpStatus.BAD_REQUEST, Common.VALIDATION_FAILED_TITLE,
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
        ProblemDetail body = ProblemDetails.of(HttpStatus.BAD_REQUEST, Common.MALFORMED_REQUEST_TITLE,
                Common.MALFORMED_REQUEST_DETAIL, ErrorCodes.MALFORMED_REQUEST);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ProblemDetails.of(HttpStatus.BAD_REQUEST, Common.INVALID_PARAMETER_TITLE,
                Common.INVALID_PARAMETER_DETAIL.formatted(ex.getPropertyName()), ErrorCodes.INVALID_PARAMETER);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex, HttpHeaders headers,
                                                                          HttpStatusCode status, WebRequest request) {
        if (ex instanceof MissingRequestHeaderException missing) {
            ProblemDetail body = ProblemDetails.of(HttpStatus.BAD_REQUEST, Common.MISSING_HEADER_TITLE,
                    Common.MISSING_HEADER_DETAIL.formatted(missing.getHeaderName()), ErrorCodes.MISSING_HEADER);
            return handleExceptionInternal(ex, body, headers, status, request);
        }
        return super.handleServletRequestBindingException(ex, headers, status, request);
    }

    // Constraints on @RequestHeader / @PathVariable parameters
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpHeaders headers,
                                                                            HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ProblemDetails.of(HttpStatus.BAD_REQUEST, Common.VALIDATION_FAILED_TITLE,
                Common.VALIDATION_FAILED_DETAIL, ErrorCodes.VALIDATION_ERROR);
        List<Map<String, String>> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream().map(error -> Map.of(
                        Problem.VIOLATION_FIELD, parameterName(result.getMethodParameter()),
                        Problem.VIOLATION_CODE, toConstraintCode(error.getCodes() != null ? lastCode(error.getCodes()) : null),
                        Problem.VIOLATION_MESSAGE, String.valueOf(error.getDefaultMessage()))))
                .toList();
        body.setProperty(Problem.ERRORS, errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    // Spring MVC errors (404, 405, 415...): same contract, our messages.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem && !ProblemDetails.hasCode(problem)) {
            FrameworkError error = FrameworkError.of(statusCode);
            problem.setTitle(error.title());
            problem.setDetail(error.detail());
            ProblemDetails.enrich(problem, error.code());
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

    private static Map<String, String> toViolation(FieldError error) {
        String message = error.isBindingFailure() ? Validation.INVALID_VALUE : String.valueOf(error.getDefaultMessage());
        return Map.of(
                Problem.VIOLATION_FIELD, error.getField(),
                Problem.VIOLATION_CODE, toConstraintCode(error.getCode()),
                Problem.VIOLATION_MESSAGE, message);
    }

    // Header name for @RequestHeader, otherwise the Java parameter name
    private static String parameterName(MethodParameter parameter) {
        RequestHeader header = parameter.getParameterAnnotation(RequestHeader.class);
        return header != null && !header.value().isEmpty() ? header.value() : String.valueOf(parameter.getParameterName());
    }

    // Validation codes go from specific to generic ("Pattern.x.y" ... "Pattern"): keep the constraint name
    private static String lastCode(String[] codes) {
        return codes[codes.length - 1];
    }

    // "NotBlank" -> "NOT_BLANK", "typeMismatch" -> "TYPE_MISMATCH"
    private static String toConstraintCode(@Nullable String constraint) {
        if (constraint == null) {
            return ErrorCodes.VALIDATION_ERROR;
        }
        return constraint.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }
}
