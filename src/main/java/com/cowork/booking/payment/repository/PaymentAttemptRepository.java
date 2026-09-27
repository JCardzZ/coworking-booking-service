package com.cowork.booking.payment.repository;

import com.cowork.booking.payment.model.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    List<PaymentAttempt> findByReservationIdOrderByCreatedAtAsc(Long reservationId);
}
