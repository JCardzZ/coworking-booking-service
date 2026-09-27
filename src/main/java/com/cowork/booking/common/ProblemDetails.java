package com.cowork.booking.common;

import com.cowork.booking.common.AppConstants.Problem;
import com.cowork.booking.common.AppConstants.Tracing;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

/** Builds the API error body; shared by the MVC advice and the security handlers. */
public final class ProblemDetails {

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatus status, String title, @Nullable String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        enrich(problem, code);
        return problem;
    }

    public static void enrich(ProblemDetail problem, String code) {
        problem.setType(type(code));
        problem.setProperty(Problem.CODE, code);
        problem.setProperty(Problem.TIMESTAMP, Instant.now());
        String traceId = MDC.get(Tracing.MDC_KEY);
        if (traceId != null) {
            problem.setProperty(Problem.TRACE_ID, traceId);
        }
    }

    public static boolean hasCode(ProblemDetail problem) {
        return problem.getProperties() != null && problem.getProperties().containsKey(Problem.CODE);
    }

    // Per-environment URL; proxy-aware via server.forward-headers-strategy.
    private static URI type(String code) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(Problem.ERRORS_PATH + Problem.slug(code))
                .build()
                .toUri();
    }
}
