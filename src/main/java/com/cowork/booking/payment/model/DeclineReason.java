package com.cowork.booking.payment.model;

import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Messages;

import java.util.Arrays;

public enum DeclineReason {
    INSUFFICIENT_FUNDS(ErrorCodes.PAYMENT_INSUFFICIENT_FUNDS, Messages.Payment.INSUFFICIENT_FUNDS),
    CARD_EXPIRED(ErrorCodes.PAYMENT_CARD_EXPIRED, Messages.Payment.CARD_EXPIRED),
    INVALID_PAYMENT_METHOD(ErrorCodes.PAYMENT_METHOD_INVALID, Messages.Payment.METHOD_INVALID),
    // reasons we don't map yet
    OTHER(ErrorCodes.PAYMENT_DECLINED, Messages.Payment.DECLINED);

    private final String errorCode;
    private final String message;

    DeclineReason(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public String errorCode() {
        return errorCode;
    }

    public String message() {
        return message;
    }

    public static DeclineReason fromProvider(String reason) {
        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(reason))
                .findFirst()
                .orElse(OTHER);
    }
}
