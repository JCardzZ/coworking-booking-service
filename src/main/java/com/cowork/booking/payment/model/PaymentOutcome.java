package com.cowork.booking.payment.model;

public enum PaymentOutcome {
    APPROVED,
    DECLINED,
    // provider down, too slow or circuit open
    UNAVAILABLE
}
