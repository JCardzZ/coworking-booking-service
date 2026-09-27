package com.cowork.booking.payment.client;

import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.common.AppConstants.Messages;
import com.cowork.booking.payment.model.PaymentResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// retry wraps the circuit breaker: each attempt counts for the circuit, and an open circuit isn't retried
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentClient {

    static final String RESILIENCE_INSTANCE = "payment";
    static final String VALIDATE_PATH = "/payments/validate";

    private final RestClient paymentRestClient;

    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "unavailable")
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public PaymentResult validate(ProviderPaymentRequest request, String idempotencyKey) {
        ProviderPaymentResponse response = paymentRestClient.post()
                .uri(VALIDATE_PATH)
                // same key on every retry so the provider doesn't charge twice
                .header(Api.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .body(request)
                .retrieve()
                .body(ProviderPaymentResponse.class);
        if (response == null) {
            throw new IllegalStateException(Messages.Payment.EMPTY_PROVIDER_RESPONSE);
        }
        return response.toResult();
    }

    // timeout, 5xx after retrying or open circuit: the reservation stays PENDING_PAYMENT
    @SuppressWarnings("unused")
    private PaymentResult unavailable(ProviderPaymentRequest request, String idempotencyKey, Throwable ex) {
        log.warn(Messages.Payment.LOG_UNAVAILABLE, request.reservationId(), ex.toString());
        return PaymentResult.unavailable();
    }
}
