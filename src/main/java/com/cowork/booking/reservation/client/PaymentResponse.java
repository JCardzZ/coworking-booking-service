package com.cowork.booking.reservation.client;

public record PaymentResponse(String status) {

    static final String APPROVED = "APPROVED";

    boolean approved() {
        return APPROVED.equalsIgnoreCase(status);
    }
}
