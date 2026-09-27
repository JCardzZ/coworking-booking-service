package com.cowork.booking.common;

import com.cowork.booking.common.AppConstants.Tracing;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/** Reuses or generates X-Correlation-Id and adds it to logs, the response and error bodies. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    // Safe charset only: prevents log/header injection.
    private static final Pattern VALID_ID = Pattern.compile(Tracing.VALID_ID_REGEX);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = resolve(request.getHeader(Tracing.CORRELATION_ID_HEADER));
        MDC.put(Tracing.MDC_KEY, correlationId);
        response.setHeader(Tracing.CORRELATION_ID_HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(Tracing.MDC_KEY);
        }
    }

    private static String resolve(@Nullable String incoming) {
        return incoming != null && VALID_ID.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();
    }
}
