package com.cowork.booking.payment.client;

import com.cowork.booking.payment.dto.PaymentMethod;

import java.math.BigDecimal;

public record ProviderPaymentRequest(Long reservationId, Long userId, BigDecimal amount, String currency,
                                     PaymentMethod paymentMethod) {
}
