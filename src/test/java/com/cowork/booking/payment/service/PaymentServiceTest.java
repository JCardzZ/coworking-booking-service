package com.cowork.booking.payment.service;

import com.cowork.booking.common.AppConstants.Payments;
import com.cowork.booking.payment.client.PaymentClient;
import com.cowork.booking.payment.client.ProviderPaymentRequest;
import com.cowork.booking.payment.dto.BankTransferPayment;
import com.cowork.booking.payment.dto.CardPayment;
import com.cowork.booking.payment.dto.PaymentMethod;
import com.cowork.booking.payment.mapper.PaymentAttemptMapper;
import com.cowork.booking.payment.model.DeclineReason;
import com.cowork.booking.payment.model.PaymentAttempt;
import com.cowork.booking.payment.model.PaymentOutcome;
import com.cowork.booking.payment.model.PaymentResult;
import com.cowork.booking.payment.repository.PaymentAttemptRepository;
import com.cowork.booking.reservation.model.Reservation;
import com.cowork.booking.space.model.Space;
import com.cowork.booking.space.model.SpaceType;
import com.cowork.booking.user.model.User;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static com.cowork.booking.user.UserFixtures.role;
import static com.cowork.booking.user.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final PaymentMethod CARD = new CardPayment("tok_visa_4242");

    @Mock
    private PaymentClient paymentClient;
    @Mock
    private PaymentAttemptRepository attemptRepository;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private PaymentService service;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentClient, attemptRepository, new PaymentAttemptMapper(), meterRegistry);
        Space space = new Space("Sala Andes", SpaceType.MEETING_ROOM, 8, "Piso 2", new BigDecimal("25.00"));
        User owner = user(7L, "ana@test.com", role("USER"));
        reservation = new Reservation(space, owner, Instant.parse("2027-01-15T09:00:00Z"),
                Instant.parse("2027-01-15T11:00:00Z"), new BigDecimal("50.00"), "key-12345678");
        ReflectionTestUtils.setField(reservation, "id", 10L);
    }

    @Test
    void sendsTheMethodAndAmountAndRecordsTheAttemptMasked() {
        when(paymentClient.validate(any(), anyString())).thenReturn(PaymentResult.approved("pay_abc123"));

        PaymentResult result = service.charge(reservation, CARD);

        assertThat(result.outcome()).isEqualTo(PaymentOutcome.APPROVED);
        ArgumentCaptor<ProviderPaymentRequest> request = ArgumentCaptor.forClass(ProviderPaymentRequest.class);
        verify(paymentClient).validate(request.capture(), anyString());
        assertThat(request.getValue().amount()).isEqualByComparingTo("50.00");
        assertThat(request.getValue().currency()).isEqualTo(Payments.CURRENCY);
        assertThat(request.getValue().paymentMethod()).isEqualTo(CARD);

        ArgumentCaptor<PaymentAttempt> attempt = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(attemptRepository).save(attempt.capture());
        assertThat(attempt.getValue().getMaskedInstrument()).isEqualTo("****4242");
        assertThat(attempt.getValue().getProviderReference()).isEqualTo("pay_abc123");
    }

    @Test
    void declinedAttemptKeepsTheReason() {
        when(paymentClient.validate(any(), anyString())).thenReturn(PaymentResult.declined(DeclineReason.CARD_EXPIRED));

        service.charge(reservation, CARD);

        ArgumentCaptor<PaymentAttempt> attempt = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(attemptRepository).save(attempt.capture());
        assertThat(attempt.getValue().getOutcome()).isEqualTo(PaymentOutcome.DECLINED);
        assertThat(attempt.getValue().getDeclineReason()).isEqualTo(DeclineReason.CARD_EXPIRED);
    }

    @Test
    void countsEachOutcomeInTheMetric() {
        when(paymentClient.validate(any(), anyString())).thenReturn(PaymentResult.unavailable());

        service.charge(reservation, CARD);
        service.charge(reservation, CARD);

        assertThat(meterRegistry.counter(Payments.METRIC_ATTEMPTS, Payments.TAG_OUTCOME, "unavailable",
                Payments.TAG_METHOD, CardPayment.TYPE).count()).isEqualTo(2);
    }

    @Test
    void sameMethodGivesTheSameIdempotencyKeyAndAnotherMethodANewOne() {
        String first = PaymentService.idempotencyKey(10L, new CardPayment("tok_visa_4242"));

        assertThat(PaymentService.idempotencyKey(10L, new CardPayment("tok_visa_4242"))).isEqualTo(first);
        assertThat(PaymentService.idempotencyKey(10L, new CardPayment("tok_master_5555"))).isNotEqualTo(first);
        assertThat(PaymentService.idempotencyKey(11L, new CardPayment("tok_visa_4242"))).isNotEqualTo(first);
        assertThat(PaymentService.idempotencyKey(10L, new BankTransferPayment("SV62CENR00000000000000700025")))
                .isNotEqualTo(first);
        assertThat(first).startsWith("rsv-10-").doesNotContain("4242");
    }
}
