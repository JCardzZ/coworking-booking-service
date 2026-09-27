package com.cowork.booking.common;

import com.cowork.booking.common.AppConstants.ErrorCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// the cases that can't be forced through a real request
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private ServletWebRequest webRequest;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/spaces");
        webRequest = new ServletWebRequest(request, new MockHttpServletResponse());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void unexpectedErrorsHideTheirDetails() {
        ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("stack trace, sql, whatever"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(code(problem)).isEqualTo(ErrorCodes.INTERNAL_ERROR);
        assertThat(problem.getDetail()).doesNotContain("sql");
    }

    @Test
    void databaseConflictsAre409() {
        assertThat(code(handler.handleDataIntegrity(new DataIntegrityViolationException("x", new SQLException("dup")))))
                .isEqualTo(ErrorCodes.DATA_INTEGRITY_VIOLATION);
        assertThat(code(handler.handleOptimisticLock(new OptimisticLockingFailureException("stale"))))
                .isEqualTo(ErrorCodes.CONCURRENT_MODIFICATION);
    }

    @Test
    void otherFrameworkErrorsGetAGenericCode() {
        assertThat(frameworkCode(HttpStatus.SERVICE_UNAVAILABLE)).isEqualTo(ErrorCodes.INTERNAL_ERROR);
        assertThat(frameworkCode(HttpStatus.NOT_ACCEPTABLE)).isEqualTo(ErrorCodes.REQUEST_ERROR);
    }

    @Test
    void bindingErrorsOtherThanAMissingHeaderUseTheGenericCode() {
        ResponseEntity<Object> response = handler.handleServletRequestBindingException(
                new ServletRequestBindingException("missing cookie"), new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

        assertThat(code((ProblemDetail) response.getBody())).isEqualTo(ErrorCodes.REQUEST_ERROR);
    }

    @Test
    @SuppressWarnings("unchecked")
    void parameterErrorWithoutCodesIsStillAValidationError() throws NoSuchMethodException {
        MessageSourceResolvable error = mock(MessageSourceResolvable.class);
        when(error.getCodes()).thenReturn(null);
        when(error.getDefaultMessage()).thenReturn("debe ser mayor que 0");
        ParameterValidationResult result = mock(ParameterValidationResult.class);
        when(result.getMethodParameter()).thenReturn(new MethodParameter(getClass().getDeclaredMethod("sample", Long.class), 0));
        when(result.getResolvableErrors()).thenReturn(List.of(error));
        HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
        when(ex.getParameterValidationResults()).thenReturn(List.of(result));

        ResponseEntity<Object> response = handler.handleHandlerMethodValidationException(ex, new HttpHeaders(),
                HttpStatus.BAD_REQUEST, webRequest);

        ProblemDetail problem = (ProblemDetail) response.getBody();
        List<Map<String, String>> errors = (List<Map<String, String>>) problem.getProperties().get("errors");
        assertThat(errors.getFirst()).containsEntry("code", ErrorCodes.VALIDATION_ERROR);
    }

    @Test
    @SuppressWarnings("unchecked")
    void headerWithoutExplicitNameFallsBackToTheParameterName() throws NoSuchMethodException {
        MessageSourceResolvable error = mock(MessageSourceResolvable.class);
        when(error.getCodes()).thenReturn(new String[]{"Pattern.token", "Pattern"});
        ParameterValidationResult result = mock(ParameterValidationResult.class);
        when(result.getMethodParameter()).thenReturn(new MethodParameter(getClass().getDeclaredMethod("withHeader", String.class), 0));
        when(result.getResolvableErrors()).thenReturn(List.of(error));
        HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
        when(ex.getParameterValidationResults()).thenReturn(List.of(result));

        ResponseEntity<Object> response = handler.handleHandlerMethodValidationException(ex, new HttpHeaders(),
                HttpStatus.BAD_REQUEST, webRequest);

        List<Map<String, String>> errors = (List<Map<String, String>>) ((ProblemDetail) response.getBody()).getProperties().get("errors");
        assertThat(errors.getFirst()).containsEntry("code", "PATTERN").doesNotContainValue("");
    }

    @Test
    void bodiesThatAreNotProblemsAreLeftAlone() {
        ResponseEntity<Object> response = handler.handleExceptionInternal(new Exception(), "texto", new HttpHeaders(),
                HttpStatus.BAD_REQUEST, webRequest);

        assertThat(response.getBody()).isEqualTo("texto");
    }

    @Test
    void nothingIsWrittenOnceTheResponseIsCommitted() {
        MockHttpServletResponse committed = new MockHttpServletResponse();
        committed.setCommitted(true);
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest(), committed);

        assertThat(handler.handleExceptionInternal(new Exception(), ProblemDetail.forStatus(HttpStatus.BAD_REQUEST),
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request)).isNull();
    }

    @SuppressWarnings("unused")
    private void sample(@PathVariable Long id) {
    }

    @SuppressWarnings("unused")
    private void withHeader(@RequestHeader String token) {
    }

    private String frameworkCode(HttpStatus status) {
        ResponseEntity<Object> response = handler.handleExceptionInternal(new Exception(), ProblemDetail.forStatus(status),
                new HttpHeaders(), status, webRequest);
        return code((ProblemDetail) response.getBody());
    }

    private static String code(ProblemDetail problem) {
        return (String) problem.getProperties().get("code");
    }
}
