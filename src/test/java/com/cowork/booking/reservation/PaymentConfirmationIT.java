package com.cowork.booking.reservation;

import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.reservation.model.ReservationStatus;
import com.cowork.booking.reservation.repository.ReservationRepository;
import com.cowork.booking.support.IntegrationTest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// WireMock answers by amount (see wiremock/mappings): >= 1000 declined, 555 -> 503, 333 -> 3s delay, anything else approved
@ExtendWith(OutputCaptureExtension.class)
class PaymentConfirmationIT extends IntegrationTest {

    @Autowired
    private ReservationRepository reservationRepository;

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

        mockMvc.perform(get("/actuator/circuitbreakers").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.circuitBreakers.payment.state").value("OPEN"));
        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // one hour, so the amount sent to the provider equals the hourly rate
    private Long reservationAt(int hourlyRate) throws Exception {
        Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        return createReservation(createSpace(hourlyRate), startAt, startAt.plus(1, ChronoUnit.HOURS));
    }

    private ReservationStatus statusOf(Long reservationId) {
        return reservationRepository.findById(reservationId).orElseThrow().getStatus();
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (!condition.getAsBoolean() && Instant.now().isBefore(deadline)) {
            Thread.sleep(50);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }
}
