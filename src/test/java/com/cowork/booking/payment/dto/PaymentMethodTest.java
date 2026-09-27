package com.cowork.booking.payment.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMethodTest {

    @Test
    void onlyTheLastFourCharactersAreShown() {
        assertThat(new CardPayment("tok_visa_4242").masked()).isEqualTo("****4242");
        assertThat(new BankTransferPayment("SV62CENR00000000000000700025").masked()).isEqualTo("****0025");
    }

    @Test
    void toStringNeverPrintsTheFullValue() {
        assertThat(new CardPayment("tok_visa_4242").toString()).doesNotContain("tok_visa").contains("****4242");
        assertThat(new BankTransferPayment("SV62CENR00000000000000700025").toString()).doesNotContain("SV62");
    }
}
