package com.cowork.booking.payment.model;

import com.cowork.booking.common.AuditableEntity;
import com.cowork.booking.payment.dto.PaymentMethod;
import com.cowork.booking.reservation.model.Reservation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static com.cowork.booking.common.AppConstants.Limits.ENUM_MAX;
import static com.cowork.booking.common.AppConstants.Limits.MONEY_FRACTION_DIGITS;
import static com.cowork.booking.common.AppConstants.Limits.MONEY_INTEGER_DIGITS;

@Entity
@Table(name = "payment_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttempt extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "method_type", nullable = false, length = ENUM_MAX)
    private String methodType;

    // never the full token or account
    @Column(name = "masked_instrument", nullable = false)
    private String maskedInstrument;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(nullable = false, precision = MONEY_INTEGER_DIGITS + MONEY_FRACTION_DIGITS, scale = MONEY_FRACTION_DIGITS)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = ENUM_MAX)
    private PaymentOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "decline_reason", length = ENUM_MAX)
    private DeclineReason declineReason;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    public PaymentAttempt(Reservation reservation, PaymentMethod method, String idempotencyKey, BigDecimal amount,
                          PaymentResult result, long durationMs) {
        this.reservation = reservation;
        this.methodType = method.type();
        this.maskedInstrument = method.masked();
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.outcome = result.outcome();
        this.declineReason = result.declineReason();
        this.providerReference = result.providerReference();
        this.durationMs = durationMs;
    }
}
