package com.cowork.booking.reservation;

import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.config.SecurityProperties;
import com.cowork.booking.reservation.dto.CreateReservationRequest;
import com.cowork.booking.reservation.model.ReservationStatus;
import com.cowork.booking.reservation.repository.ReservationRepository;
import com.cowork.booking.space.dto.SpaceRequest;
import com.cowork.booking.space.model.SpaceType;
import com.cowork.booking.support.TestcontainersConfig;
import com.cowork.booking.user.dto.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// WireMock answers by amount (see wiremock/mappings): >= 1000 declined, 555 -> 503, 333 -> 3s delay, anything else approved
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class PaymentConfirmationIT {

    private static final String RESERVATIONS = Api.BASE_PATH + Api.RESERVATIONS;
    private static final String PAYMENT_CIRCUIT = "payment";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired
    private SecurityProperties securityProperties;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        circuitBreakerRegistry.circuitBreaker(PAYMENT_CIRCUIT).reset();
        adminToken = login();
    }

    @Test
    void approvedPaymentConfirmsTheReservation() throws Exception {
        Long reservationId = reservationAt(20);

        confirm(reservationId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReservationStatus.CONFIRMED.name()));

        assertThat(statusOf(reservationId)).isEqualTo(ReservationStatus.CONFIRMED);
        confirm(reservationId).andExpect(status().isConflict());
    }

    @Test
    void confirmationIsNotifiedAsynchronously(CapturedOutput output) throws Exception {
        Long reservationId = reservationAt(20);

        confirm(reservationId).andExpect(status().isOk());

        String notification = "reserva " + reservationId + " confirmada";
        waitUntil(() -> output.getOut().contains(notification));
        String line = output.getOut().lines().filter(l -> l.contains(notification)).findFirst().orElseThrow();
        assertThat(line).containsPattern("\\[\\s*async-\\d+]");
    }

    @Test
    void declinedPaymentKeepsTheReservationPending() throws Exception {
        Long reservationId = reservationAt(1000);

        confirm(reservationId)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCodes.PAYMENT_DECLINED));

        assertThat(statusOf(reservationId)).isEqualTo(ReservationStatus.PENDING_PAYMENT);
    }

    @Test
    void slowProviderFallsBackToPendingPayment() throws Exception {
        Long reservationId = reservationAt(333);

        confirm(reservationId)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value(ReservationStatus.PENDING_PAYMENT.name()));

        assertThat(statusOf(reservationId)).isEqualTo(ReservationStatus.PENDING_PAYMENT);
    }

    @Test
    void failingProviderOpensTheCircuit() throws Exception {
        for (int i = 0; i < 5; i++) {
            confirm(reservationAt(555)).andExpect(status().isAccepted());
        }
        assertThat(circuitBreakerRegistry.circuitBreaker(PAYMENT_CIRCUIT).getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // the provider would approve this one, but the open circuit doesn't even call it
        Long reservationId = reservationAt(20);
        confirm(reservationId).andExpect(status().isAccepted());
        assertThat(statusOf(reservationId)).isEqualTo(ReservationStatus.PENDING_PAYMENT);

        mockMvc.perform(get("/actuator/circuitbreakers").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.circuitBreakers.payment.state").value("OPEN"));
        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private Long reservationAt(int hourlyRate) throws Exception {
        SpaceRequest space = new SpaceRequest("Sala pago " + UUID.randomUUID(), SpaceType.DESK, 1, "Piso 1",
                BigDecimal.valueOf(hourlyRate));
        Long spaceId = idOf(mockMvc.perform(post(Api.BASE_PATH + Api.SPACES)
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(space)))
                .andExpect(status().isCreated()));

        // one hour, so the amount sent to the provider equals the hourly rate
        Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        CreateReservationRequest reservation = new CreateReservationRequest(spaceId, startAt, startAt.plus(1, ChronoUnit.HOURS));
        return idOf(mockMvc.perform(post(RESERVATIONS)
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .header(Api.IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservation)))
                .andExpect(status().isCreated()));
    }

    private ResultActions confirm(Long reservationId) throws Exception {
        return mockMvc.perform(post(RESERVATIONS + "/{id}" + Api.CONFIRM, reservationId)
                .header(HttpHeaders.AUTHORIZATION, bearer()));
    }

    private ReservationStatus statusOf(Long reservationId) {
        return reservationRepository.findById(reservationId).orElseThrow().getStatus();
    }

    private String login() throws Exception {
        SecurityProperties.Admin admin = securityProperties.admin();
        String body = mockMvc.perform(post(Api.BASE_PATH + Api.AUTH + Api.LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(admin.email(), admin.password()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private Long idOf(ResultActions result) throws Exception {
        JsonNode body = objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (!condition.getAsBoolean() && Instant.now().isBefore(deadline)) {
            Thread.sleep(50);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    private String bearer() {
        return "Bearer " + adminToken;
    }
}
