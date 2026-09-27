package com.cowork.booking.reservation.model;

import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.BusinessRuleException;
import com.cowork.booking.space.model.Space;
import com.cowork.booking.space.model.SpaceType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static com.cowork.booking.user.UserFixtures.role;
import static com.cowork.booking.user.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationStateTest {

    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");

    @Test
    void newReservationIsPendingPayment() {
        assertThat(reservation().getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
    }

    @Test
    void pendingCanBeConfirmedAndThenCancelled() {
        Reservation reservation = reservation();
        reservation.confirm();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        reservation.cancel(NOW);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getCancelledAt()).isEqualTo(NOW);
    }

    @Test
    void confirmedCannotBeConfirmedAgain() {
        Reservation reservation = reservation();
        reservation.confirm();

        assertThatThrownBy(reservation::confirm)
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo(ErrorCodes.INVALID_RESERVATION_STATE);
    }

    @Test
    void cancelledIsFinal() {
        Reservation reservation = reservation();
        reservation.cancel(NOW);

        assertThatThrownBy(reservation::confirm).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> reservation.cancel(NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo(ErrorCodes.INVALID_RESERVATION_STATE);
    }

    @Test
    void cannotCancelOnceStarted() {
        Reservation reservation = reservation();

        assertThatThrownBy(() -> reservation.cancel(Instant.parse("2026-10-01T09:30:00Z")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo(ErrorCodes.RESERVATION_ALREADY_STARTED);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
    }

    private static Reservation reservation() {
        Space space = new Space("Sala Andes", SpaceType.MEETING_ROOM, 8, "Piso 2", new BigDecimal("25.00"));
        return new Reservation(space, user(2L, "ana@coworking.com", role("USER")),
                Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T11:00:00Z"),
                new BigDecimal("50.00"), "key-12345678");
    }
}
