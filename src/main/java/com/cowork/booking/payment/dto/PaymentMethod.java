package com.cowork.booking.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

// "type" picks the subtype; a new payment method is just a new record
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardPayment.class, name = CardPayment.TYPE),
        @JsonSubTypes.Type(value = BankTransferPayment.class, name = BankTransferPayment.TYPE)
})
@Schema(oneOf = {CardPayment.class, BankTransferPayment.class}, discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = CardPayment.TYPE, schema = CardPayment.class),
                @DiscriminatorMapping(value = BankTransferPayment.TYPE, schema = BankTransferPayment.class)
        })
public sealed interface PaymentMethod permits CardPayment, BankTransferPayment {

    int VISIBLE_CHARS = 4;
    String MASK = "****";

    @JsonIgnore
    String type();

    // full value, only goes to the provider
    @JsonIgnore
    String instrument();

    // what we log and return
    @JsonIgnore
    default String masked() {
        String instrument = instrument();
        return instrument.length() <= VISIBLE_CHARS
                ? MASK
                : MASK + instrument.substring(instrument.length() - VISIBLE_CHARS);
    }
}
