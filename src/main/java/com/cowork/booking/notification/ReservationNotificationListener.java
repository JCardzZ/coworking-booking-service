package com.cowork.booking.notification;

import com.cowork.booking.common.AppConstants.Messages;
import com.cowork.booking.reservation.event.ReservationConfirmedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Sends the confirmation to the user. Mocked with a log line: swapping it for email or a queue doesn't touch the reservation code. */
@Slf4j
@Component
public class ReservationNotificationListener {

    // after commit, so a rolled back confirmation is never notified; async, so it doesn't slow down the response
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
        log.info(Messages.Notification.LOG_RESERVATION_CONFIRMED, event.userEmail(), event.reservationId(),
                event.spaceName(), event.startAt(), event.endAt(), event.totalAmount());
    }
}
