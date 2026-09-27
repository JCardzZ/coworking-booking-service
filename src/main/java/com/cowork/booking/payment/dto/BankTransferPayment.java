package com.cowork.booking.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static com.cowork.booking.common.AppConstants.Limits.ACCOUNT_NUMBER_PATTERN;
import static com.cowork.booking.common.AppConstants.Messages.Validation.ACCOUNT_NUMBER_FORMAT;
import static com.cowork.booking.common.AppConstants.Messages.Validation.NOT_BLANK;

@Schema(description = "Transferencia desde una cuenta bancaria")
public record BankTransferPayment(
        @Schema(description = "Número de cuenta en formato IBAN", example = "SV62CENR00000000000000700025")
        @NotBlank(message = NOT_BLANK) @Pattern(regexp = ACCOUNT_NUMBER_PATTERN, message = ACCOUNT_NUMBER_FORMAT)
        String accountNumber
) implements PaymentMethod {

    public static final String TYPE = "BANK_TRANSFER";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public String instrument() {
        return accountNumber;
    }

    @Override
    public String toString() {
        return "BankTransferPayment[accountNumber=%s]".formatted(masked());
    }
}
