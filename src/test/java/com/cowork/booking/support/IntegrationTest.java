package com.cowork.booking.support;

import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.config.SecurityProperties;
import com.cowork.booking.reservation.dto.CreateReservationRequest;
import com.cowork.booking.space.dto.SpaceRequest;
import com.cowork.booking.space.model.SpaceType;
import com.cowork.booking.user.dto.LoginRequest;
import com.cowork.booking.user.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full app over HTTP with a real JWT, against the Postgres and WireMock containers. All ITs share one context. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class IntegrationTest {

    protected static final String PAYMENT_CIRCUIT = "payment";
    protected static final String RESERVATIONS = Api.BASE_PATH + Api.RESERVATIONS;

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired
    private SecurityProperties securityProperties;

    protected String adminToken;

    @BeforeEach
    void setUpIntegrationTest() throws Exception {
        circuitBreakerRegistry.circuitBreaker(PAYMENT_CIRCUIT).reset();
        SecurityProperties.Admin admin = securityProperties.admin();
        adminToken = login(admin.email(), admin.password());
    }

    protected Long createSpace(int hourlyRate) throws Exception {
        SpaceRequest space = new SpaceRequest("Sala " + UUID.randomUUID(), SpaceType.DESK, 1, "Piso 1",
                BigDecimal.valueOf(hourlyRate));
        return idOf(mockMvc.perform(post(Api.BASE_PATH + Api.SPACES)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(space)))
                .andExpect(status().isCreated()));
    }

    protected ResultActions reserve(String token, Long spaceId, Instant startAt, Instant endAt) throws Exception {
        return mockMvc.perform(post(RESERVATIONS)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .header(Api.IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReservationRequest(spaceId, startAt, endAt))));
    }

    protected Long createReservation(Long spaceId, Instant startAt, Instant endAt) throws Exception {
        return idOf(reserve(adminToken, spaceId, startAt, endAt).andExpect(status().isCreated()));
    }

    protected ResultActions confirm(Long reservationId) throws Exception {
        return mockMvc.perform(post(RESERVATIONS + "/{id}" + Api.CONFIRM, reservationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)));
    }

    protected ResultActions cancel(Long reservationId) throws Exception {
        return mockMvc.perform(post(RESERVATIONS + "/{id}" + Api.CANCEL, reservationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)));
    }

    protected String registerUser() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.com";
        String password = "User1234!";
        mockMvc.perform(post(Api.BASE_PATH + Api.AUTH + Api.REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, password, "Usuario IT"))))
                .andExpect(status().isCreated());
        return login(email, password);
    }

    protected String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post(Api.BASE_PATH + Api.AUTH + Api.LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    protected Long idOf(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
