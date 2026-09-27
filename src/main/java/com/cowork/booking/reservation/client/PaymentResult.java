package com.cowork.booking.reservation.client;

public enum PaymentResult {
    APPROVED,
    DECLINED,
    /** provider down, slow or circuit open */
    UNAVAILABLE
}
