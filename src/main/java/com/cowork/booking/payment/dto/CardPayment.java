package com.cowork.booking.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static com.cowork.booking.common.AppConstants.Limits.CARD_TOKEN_PATTERN;
import static com.cowork.booking.common.AppConstants.Messages.Validation.CARD_TOKEN_FORMAT;
import static com.cowork.booking.common.AppConstants.Messages.Validation.NOT_BLANK;

// we never see the card number, only the provider's token
@Schema(description = "Tarjeta tokenizada por el proveedor de pagos")
public record CardPayment(
        @Schema(description = "Token de la tarjeta emitido por el proveedor", example = "tok_visa_4242")
        @NotBlank(message = NOT_BLANK) @Pattern(regexp = CARD_TOKEN_PATTERN, message = CARD_TOKEN_FORMAT)
        String token
) implements PaymentMethod {

    public static final String TYPE = "CARD";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public String instrument() {
        return token;
    }

    // keep the token out of the logs
    @Override
    public String toString() {
        return "CardPayment[token=%s]".formatted(masked());
    }
}
