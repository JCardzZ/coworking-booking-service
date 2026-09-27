package com.cowork.booking.reservation.model;

public enum ReservationStatus {
    PENDING_PAYMENT(new PendingPaymentState()),
    CONFIRMED(new ConfirmedState()),
    CANCELLED(new CancelledState());

    private final ReservationState state;

    ReservationStatus(ReservationState state) {
        this.state = state;
    }

    ReservationState state() {
        return state;
    }
}
