package com.cowork.booking.reservation.dto;

import com.cowork.booking.payment.dto.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import static com.cowork.booking.common.AppConstants.Messages.Validation.REQUIRED;

public record ConfirmReservationRequest(@Valid @NotNull(message = REQUIRED) PaymentMethod paymentMethod) {
}
