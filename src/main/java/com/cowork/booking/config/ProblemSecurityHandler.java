package com.cowork.booking.config;

import com.cowork.booking.common.ApiDocs;
import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Messages.Common;
import com.cowork.booking.common.AppConstants.Security;
import com.cowork.booking.common.ProblemDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/** Same error body for the 401/403 coming from the security filters. */
@Component
@RequiredArgsConstructor
public class ProblemSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, Security.TOKEN_TYPE);
        write(request, response, ProblemDetails.of(HttpStatus.UNAUTHORIZED, Common.UNAUTHORIZED_TITLE,
                Common.UNAUTHORIZED_DETAIL, ErrorCodes.UNAUTHORIZED));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(request, response, ProblemDetails.of(HttpStatus.FORBIDDEN, Common.FORBIDDEN_TITLE,
                Common.FORBIDDEN_DETAIL, ErrorCodes.FORBIDDEN));
    }

    private void write(HttpServletRequest request, HttpServletResponse response, ProblemDetail problem) throws IOException {
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(problem.getStatus());
        response.setContentType(ApiDocs.PROBLEM_JSON);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
