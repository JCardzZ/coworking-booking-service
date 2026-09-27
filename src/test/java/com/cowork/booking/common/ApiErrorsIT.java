package com.cowork.booking.common;

import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Tracing;
import com.cowork.booking.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// every error goes out with the same shape: status, code and our own message
class ApiErrorsIT extends IntegrationTest {

    private static final String SPACES = Api.BASE_PATH + Api.SPACES;

    @Test
    void unknownResourceSaysWhichOne() throws Exception {
        perform(get(SPACES + "/999999"), adminToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCodes.RESOURCE_NOT_FOUND))
                .andExpect(jsonPath("$.resourceType").value("Space"))
                .andExpect(jsonPath("$.type").value(containsString("/errors/resource-not-found")));
    }

    @Test
    void duplicateNamePointsToTheField() throws Exception {
        String body = """
                {"name": "Sala repetida %d", "type": "DESK", "capacity": 1, "location": "Piso 1", "hourlyRate": 10}
                """.formatted(System.nanoTime());
        perform(post(SPACES).contentType(MediaType.APPLICATION_JSON).content(body), adminToken).andExpect(status().isCreated());

        perform(post(SPACES).contentType(MediaType.APPLICATION_JSON).content(body), adminToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodes.SPACE_NAME_TAKEN))
                .andExpect(jsonPath("$.conflictingField").value("name"));
    }

    @Test
    void badRequestsAreExplained() throws Exception {
        perform(get(SPACES).param("sort", "foo"), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.INVALID_SORT));
        perform(get(SPACES + "/abc"), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.INVALID_PARAMETER));
        perform(get(SPACES).param("minCapacity", "abc"), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.errors[0].field").value("minCapacity"));
        perform(post(SPACES).contentType(MediaType.APPLICATION_JSON).content("{"), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.MALFORMED_REQUEST));
    }

    @Test
    void reservationHeadersAreChecked() throws Exception {
        Instant start = Instant.now().plus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        String body = """
                {"spaceId": 1, "startAt": "%s", "endAt": "%s"}""".formatted(start, start.plus(1, ChronoUnit.HOURS));

        perform(post(RESERVATIONS).contentType(MediaType.APPLICATION_JSON).content(body), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.MISSING_HEADER));
        perform(post(RESERVATIONS).header(Api.IDEMPOTENCY_KEY_HEADER, "bad!")
                        .contentType(MediaType.APPLICATION_JSON).content(body), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.errors[0].field").value(Api.IDEMPOTENCY_KEY_HEADER));
    }

    @Test
    void springMvcErrorsUseOurFormat() throws Exception {
        perform(get(Api.BASE_PATH + "/nope"), adminToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCodes.ROUTE_NOT_FOUND));
        perform(patch(SPACES + "/1"), adminToken)
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(ErrorCodes.METHOD_NOT_ALLOWED));
        perform(post(SPACES).contentType(MediaType.TEXT_PLAIN).content("hola"), adminToken)
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void securityErrorsComeFromTheFilterWithTheSameShape() throws Exception {
        perform(get(SPACES), null)
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));
        perform(get("/actuator/metrics"), registerUser())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        perform(post(Api.BASE_PATH + Api.AUTH + Api.LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"nadie@test.com\", \"password\": \"Incorrecta1\"}"), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.INVALID_CREDENTIALS));
    }

    @Test
    void correlationIdIsKeptWhenValidAndReplacedWhenNot() throws Exception {
        perform(get(SPACES).header(Tracing.CORRELATION_ID_HEADER, "mi-id-123"), adminToken)
                .andExpect(header().string(Tracing.CORRELATION_ID_HEADER, "mi-id-123"));
        perform(get(SPACES).header(Tracing.CORRELATION_ID_HEADER, "no vale!"), adminToken)
                .andExpect(header().string(Tracing.CORRELATION_ID_HEADER, not("no vale!")));
    }

    @Test
    void openApiShowsDomainPathsUnderTheVersionedServer() throws Exception {
        perform(get("/v3/api-docs"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/spaces']").exists())
                .andExpect(jsonPath("$.servers[0].url").value(endsWith(Api.BASE_PATH)))
                .andExpect(jsonPath("$.paths['/spaces'].get.responses['401']").exists());
    }
}
