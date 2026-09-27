package com.cowork.booking.reservation.model;

import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Messages;
import com.cowork.booking.common.AuditableEntity;
import com.cowork.booking.common.BusinessRuleException;
import com.cowork.booking.space.model.Space;
import com.cowork.booking.user.model.User;
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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

import static com.cowork.booking.common.AppConstants.Limits.ENUM_MAX;
import static com.cowork.booking.common.AppConstants.Limits.IDEMPOTENCY_KEY_MAX;
import static com.cowork.booking.common.AppConstants.Limits.MONEY_FRACTION_DIGITS;
import static com.cowork.booking.common.AppConstants.Limits.MONEY_INTEGER_DIGITS;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = ENUM_MAX)
    private ReservationStatus status;

    @Column(name = "total_amount", nullable = false, precision = MONEY_INTEGER_DIGITS + MONEY_FRACTION_DIGITS,
            scale = MONEY_FRACTION_DIGITS)
    private BigDecimal totalAmount;

    @Column(name = "idempotency_key", nullable = false, length = IDEMPOTENCY_KEY_MAX)
    private String idempotencyKey;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    private long version;

    public Reservation(Space space, User user, Instant startAt, Instant endAt, BigDecimal totalAmount,
                       String idempotencyKey) {
        this.space = space;
        this.user = user;
        this.startAt = startAt;
        this.endAt = endAt;
        this.totalAmount = totalAmount;
        this.idempotencyKey = idempotencyKey;
        this.status = ReservationStatus.PENDING_PAYMENT;
    }

    public void confirm() {
        this.status = status.state().confirm();
    }

    public void cancel(Instant now) {
        ReservationStatus next = status.state().cancel();
        if (!startAt.isAfter(now)) {
            throw new BusinessRuleException(ErrorCodes.RESERVATION_ALREADY_STARTED, Messages.Reservation.ALREADY_STARTED);
        }
        this.status = next;
        this.cancelledAt = now;
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    // replay check: same Idempotency-Key has to come with the same data
    public boolean isSameRequest(Long spaceId, Instant startAt, Instant endAt) {
        return space.getId().equals(spaceId) && this.startAt.equals(startAt) && this.endAt.equals(endAt);
    }
}
