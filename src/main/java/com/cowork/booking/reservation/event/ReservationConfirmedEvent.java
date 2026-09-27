package com.cowork.booking.reservation.event;

import com.cowork.booking.reservation.model.Reservation;

import java.math.BigDecimal;
import java.time.Instant;

/** Published once the payment is approved and the reservation is CONFIRMED. Plain values, no entities: listeners run in another thread. */
public record ReservationConfirmedEvent(Long reservationId, String userEmail, String spaceName, Instant startAt,
                                        Instant endAt, BigDecimal totalAmount) {

    public static ReservationConfirmedEvent of(Reservation reservation) {
        return new ReservationConfirmedEvent(reservation.getId(), reservation.getUser().getEmail(),
                reservation.getSpace().getName(), reservation.getStartAt(), reservation.getEndAt(),
                reservation.getTotalAmount());
    }
}
