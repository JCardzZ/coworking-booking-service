package com.cowork.booking.reservation.model;

import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Messages;
import com.cowork.booking.common.BusinessRuleException;

/** State pattern: each status knows which transitions it allows. */
sealed interface ReservationState permits PendingPaymentState, ConfirmedState, CancelledState {

    ReservationStatus status();

    default ReservationStatus confirm() {
        throw invalid(Messages.Reservation.ACTION_CONFIRM);
    }

    default ReservationStatus cancel() {
        throw invalid(Messages.Reservation.ACTION_CANCEL);
    }

    private BusinessRuleException invalid(String action) {
        return new BusinessRuleException(ErrorCodes.INVALID_RESERVATION_STATE,
                Messages.Reservation.INVALID_TRANSITION.formatted(action, status()));
    }
}

/** Waiting for payment: can be confirmed or cancelled. */
final class PendingPaymentState implements ReservationState {

    @Override
    public ReservationStatus status() {
        return ReservationStatus.PENDING_PAYMENT;
    }

    @Override
    public ReservationStatus confirm() {
        return ReservationStatus.CONFIRMED;
    }

    @Override
    public ReservationStatus cancel() {
        return ReservationStatus.CANCELLED;
    }
}

/** Paid: can only be cancelled. */
final class ConfirmedState implements ReservationState {

    @Override
    public ReservationStatus status() {
        return ReservationStatus.CONFIRMED;
    }

    @Override
    public ReservationStatus cancel() {
        return ReservationStatus.CANCELLED;
    }
}

/** Final state: no transitions. */
final class CancelledState implements ReservationState {

    @Override
    public ReservationStatus status() {
        return ReservationStatus.CANCELLED;
    }
}
