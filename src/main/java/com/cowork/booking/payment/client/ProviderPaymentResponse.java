package com.cowork.booking.payment.client;

import com.cowork.booking.payment.model.DeclineReason;
import com.cowork.booking.payment.model.PaymentResult;

record ProviderPaymentResponse(String status, String reason, String reference) {

    static final String APPROVED = "APPROVED";

    PaymentResult toResult() {
        return APPROVED.equalsIgnoreCase(status)
                ? PaymentResult.approved(reference)
                : PaymentResult.declined(DeclineReason.fromProvider(reason));
    }
}
