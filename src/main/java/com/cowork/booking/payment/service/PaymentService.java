package com.cowork.booking.payment.service;

import com.cowork.booking.common.AppConstants.Payments;
import com.cowork.booking.payment.client.PaymentClient;
import com.cowork.booking.payment.client.ProviderPaymentRequest;
import com.cowork.booking.payment.dto.PaymentAttemptResponse;
import com.cowork.booking.payment.dto.PaymentMethod;
import com.cowork.booking.payment.mapper.PaymentAttemptMapper;
import com.cowork.booking.payment.model.PaymentAttempt;
import com.cowork.booking.payment.model.PaymentResult;
import com.cowork.booking.payment.repository.PaymentAttemptRepository;
import com.cowork.booking.reservation.model.Reservation;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int FINGERPRINT_BYTES = 8;

    private final PaymentClient paymentClient;
    private final PaymentAttemptRepository attemptRepository;
    private final PaymentAttemptMapper attemptMapper;
    private final MeterRegistry meterRegistry;

    // runs outside any transaction on purpose: the attempt is saved even if the confirmation fails later
    public PaymentResult charge(Reservation reservation, PaymentMethod method) {
        String idempotencyKey = idempotencyKey(reservation.getId(), method);
        ProviderPaymentRequest request = new ProviderPaymentRequest(reservation.getId(), reservation.getUser().getId(),
                reservation.getTotalAmount(), Payments.CURRENCY, method);

        long start = System.nanoTime();
        PaymentResult result = paymentClient.validate(request, idempotencyKey);
        long durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

        attemptRepository.save(new PaymentAttempt(reservation, method, idempotencyKey, reservation.getTotalAmount(),
                result, durationMs));
        meterRegistry.counter(Payments.METRIC_ATTEMPTS,
                Payments.TAG_OUTCOME, result.outcome().name().toLowerCase(Locale.ROOT),
                Payments.TAG_METHOD, method.type()).increment();
        return result;
    }

    public List<PaymentAttemptResponse> attemptsOf(Long reservationId) {
        return attemptRepository.findByReservationIdOrderByCreatedAtAsc(reservationId).stream()
                .map(attemptMapper::toResponse)
                .toList();
    }

    // same reservation and same card give the same key; another card is a new attempt
    static String idempotencyKey(Long reservationId, PaymentMethod method) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((method.type() + ":" + method.instrument()).getBytes(StandardCharsets.UTF_8));
            String fingerprint = HexFormat.of().formatHex(hash, 0, FINGERPRINT_BYTES);
            return Payments.IDEMPOTENCY_KEY_PREFIX + reservationId + "-" + fingerprint;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
