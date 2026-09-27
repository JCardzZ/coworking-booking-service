package com.cowork.booking.payment.mapper;

import com.cowork.booking.payment.dto.PaymentAttemptResponse;
import com.cowork.booking.payment.model.PaymentAttempt;
import org.springframework.stereotype.Component;

@Component
public class PaymentAttemptMapper {

    public PaymentAttemptResponse toResponse(PaymentAttempt attempt) {
        return new PaymentAttemptResponse(attempt.getId(), attempt.getMethodType(), attempt.getMaskedInstrument(),
                attempt.getAmount(), attempt.getOutcome(), attempt.getDeclineReason(), attempt.getProviderReference(),
                attempt.getDurationMs(), attempt.getCreatedAt());
    }
}
