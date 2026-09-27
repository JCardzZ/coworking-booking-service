package com.cowork.booking.payment.model;

public record PaymentResult(PaymentOutcome outcome, String providerReference, DeclineReason declineReason) {

    public static PaymentResult approved(String providerReference) {
        return new PaymentResult(PaymentOutcome.APPROVED, providerReference, null);
    }

    public static PaymentResult declined(DeclineReason reason) {
        return new PaymentResult(PaymentOutcome.DECLINED, null, reason);
    }

    public static PaymentResult unavailable() {
        return new PaymentResult(PaymentOutcome.UNAVAILABLE, null, null);
    }
}
