package com.cowork.booking.reservation;

import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Payments;
import com.cowork.booking.payment.dto.BankTransferPayment;
import com.cowork.booking.payment.dto.CardPayment;
import com.cowork.booking.reservation.model.ReservationStatus;
import com.cowork.booking.reservation.repository.ReservationRepository;
import com.cowork.booking.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// WireMock (see wiremock/mappings): tok_insufficient_funds, tok_expired and SV00... accounts are declined,
// amount 555 -> 503, amount 333 -> 3s delay, anything else approved
@ExtendWith(OutputCaptureExtension.class)
class PaymentConfirmationIT extends IntegrationTest {

    private static final String VALID_ACCOUNT = "SV62CENR00000000000000700025";

    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private WireMockContainer paymentProvider;

    @Test
    void approvedCardConfirmsTheReservationAndKeepsTheReference() throws Exception {
        Long reservationId = reservationAt(20);

        confirm(reservationId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReservationStatus.CONFIRMED.name()))
                .andExpect(jsonPath("$.paymentReference", startsWith("pay_")));

        assertThat(statusOf(reservationId)).isEqualTo(ReservationStatus.CONFIRMED);
        confirm(reservationId).andExpect(status().isConflict());
    }

    @Test
    void bankTransferIsAlsoAccepted() throws Exception {
        confirm(reservationAt(20), new BankTransferPayment(VALID_ACCOUNT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReservationStatus.CONFIRMED.name()));
    }

    @Test
    void eachDeclineReasonHasItsOwnCode() throws Exception {
        confirm(reservationAt(20), new CardPayment("tok_insufficient_funds"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCodes.PAYMENT_INSUFFICIENT_FUNDS));
        confirm(reservationAt(20), new CardPayment("tok_expired"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCodes.PAYMENT_CARD_EXPIRED));
        confirm(reservationAt(20), new BankTransferPayment("SV00BANK0000000000000000001"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCodes.PAYMENT_METHOD_INVALID));
    }

    @Test
    void afterADeclineTheUserCanPayWithAnotherCardAndBothAttemptsAreKept() throws Exception {
        Long reservationId = reservationAt(20);

        confirm(reservationId, new CardPayment("tok_insufficient_funds")).andExpect(status().isUnprocessableEntity());
        assertThat(statusOf(reservationId)).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        confirm(reservationId).andExpect(status().isOk());

        mockMvc.perform(get(RESERVATIONS + "/{id}" + Api.PAYMENTS, reservationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].outcome").value("DECLINED"))
                .andExpect(jsonPath("$[0].declineReason").value("INSUFFICIENT_FUNDS"))
                .andExpect(jsonPath("$[0].maskedInstrument").value("****unds"))
                .andExpect(jsonPath("$[1].outcome").value("APPROVED"))
                .andExpect(jsonPath("$[1].maskedInstrument").value("****4242"));
    }

    @Test
    void invalidPaymentMethodIsRejectedBeforeCallingTheProvider() throws Exception {
        Long reservationId = reservationAt(20);

        confirmWithBody(reservationId, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR));
        confirmWithBody(reservationId, "{\"paymentMethod\":{\"type\":\"CARD\",\"token\":\"4242424242424242\"}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR));
        confirmWithBody(reservationId, "{\"paymentMethod\":{\"type\":\"CRYPTO\",\"wallet\":\"0xabc\"}}")
                .andExpect(status().isBadRequest());

        assertThat(providerCallsFor(reservationId)).isEmpty();
    }

    @Test
    void slowProviderFallsBackToPendingPayment() throws Exception {
        Long reservationId = reservationAt(333);

        confirm(reservationId)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value(ReservationStatus.PENDING_PAYMENT.name()));

        assertThat(statusOf(reservationId)).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        // a timeout isn't retried
        assertThat(providerCallsFor(reservationId)).hasSize(1);
    }

    @Test
    void providerErrorIsRetriedOnceWithTheSameIdempotencyKey() throws Exception {
        Long reservationId = reservationAt(555);

        confirm(reservationId).andExpect(status().isAccepted());

        List<JsonNode> calls = providerCallsFor(reservationId);
        assertThat(calls).hasSize(2);
        assertThat(calls).extracting(call -> call.at("/headers/" + Api.IDEMPOTENCY_KEY_HEADER).asText())
                .containsOnly(calls.getFirst().at("/headers/" + Api.IDEMPOTENCY_KEY_HEADER).asText())
                .allSatisfy(key -> assertThat(key).startsWith(Payments.IDEMPOTENCY_KEY_PREFIX + reservationId));
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
        assertThat(providerCallsFor(reservationId)).isEmpty();

        mockMvc.perform(get("/actuator/circuitbreakers").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.circuitBreakers.payment.state").value("OPEN"));
        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void attemptsAreCountedInTheMetrics() throws Exception {
        confirm(reservationAt(20)).andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics/" + Payments.METRIC_ATTEMPTS)
                        .param("tag", Payments.TAG_OUTCOME + ":approved")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value").isNumber());
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

    // one hour, so the amount sent to the provider equals the hourly rate
    private Long reservationAt(int hourlyRate) throws Exception {
        Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        return createReservation(createSpace(hourlyRate), startAt, startAt.plus(1, ChronoUnit.HOURS));
    }

    private ReservationStatus statusOf(Long reservationId) {
        return reservationRepository.findById(reservationId).orElseThrow().getStatus();
    }

    // what actually reached the provider, from WireMock's request journal
    private List<JsonNode> providerCallsFor(Long reservationId) throws Exception {
        String criteria = """
                {"method": "POST", "url": "/payments/validate",
                 "bodyPatterns": [{"matchesJsonPath": "$[?(@.reservationId == %d)]"}]}""".formatted(reservationId);
        String body = RestClient.create(paymentProvider.getBaseUrl()).post()
                .uri("/__admin/requests/find")
                .contentType(MediaType.APPLICATION_JSON)
                .body(criteria)
                .retrieve()
                .body(String.class);
        JsonNode requests = objectMapper.readTree(body).get("requests");
        return StreamSupport.stream(requests.spliterator(), false).toList();
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (!condition.getAsBoolean() && Instant.now().isBefore(deadline)) {
            Thread.sleep(50);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }
}
