package com.cowork.booking.reservation.client;

import java.math.BigDecimal;

public record PaymentRequest(Long reservationId, Long userId, BigDecimal amount) {
}
