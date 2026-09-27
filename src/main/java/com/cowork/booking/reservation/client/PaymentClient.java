package com.cowork.booking.reservation.client;

import com.cowork.booking.common.AppConstants.Messages;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Calls the external payment validation service, guarded by the "payment" circuit breaker. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentClient {

    static final String CIRCUIT_BREAKER = "payment";
    static final String VALIDATE_PATH = "/payments/validate";

    private final RestClient paymentRestClient;

    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "unavailable")
    public PaymentResult validate(PaymentRequest request) {
        PaymentResponse response = paymentRestClient.post()
                .uri(VALIDATE_PATH)
                .body(request)
                .retrieve()
                .body(PaymentResponse.class);
        return response != null && response.approved() ? PaymentResult.APPROVED : PaymentResult.DECLINED;
    }

    // timeouts, 5xx and an open circuit all end up here: the reservation just stays PENDING_PAYMENT
    @SuppressWarnings("unused")
    private PaymentResult unavailable(PaymentRequest request, Throwable ex) {
        log.warn(Messages.Reservation.LOG_PAYMENT_UNAVAILABLE, request.reservationId(), ex.toString());
        return PaymentResult.UNAVAILABLE;
    }
}
