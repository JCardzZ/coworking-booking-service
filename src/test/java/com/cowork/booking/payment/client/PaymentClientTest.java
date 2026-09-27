package com.cowork.booking.payment.client;

import com.cowork.booking.payment.dto.CardPayment;
import com.cowork.booking.payment.model.DeclineReason;
import com.cowork.booking.payment.model.PaymentOutcome;
import com.cowork.booking.payment.model.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// no Spring here, so no retry or circuit breaker
class PaymentClientTest {

    private static final ProviderPaymentRequest REQUEST = new ProviderPaymentRequest(10L, 2L, new BigDecimal("20.00"),
            "USD", new CardPayment("tok_visa_4242"));

    private MockRestServiceServer provider;
    private PaymentClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://payments");
        provider = MockRestServiceServer.bindTo(builder).build();
        client = new PaymentClient(builder.build());
    }

    @Test
    void sendsTheIdempotencyKeyAndReadsTheApproval() {
        provider.expect(requestTo("http://payments" + PaymentClient.VALIDATE_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "rsv-10-abc"))
                .andRespond(withSuccess("{\"status\": \"APPROVED\", \"reference\": \"pay_123\"}", MediaType.APPLICATION_JSON));

        PaymentResult result = client.validate(REQUEST, "rsv-10-abc");

        assertThat(result.outcome()).isEqualTo(PaymentOutcome.APPROVED);
        assertThat(result.providerReference()).isEqualTo("pay_123");
    }

    @Test
    void unknownDeclineReasonIsStillADecline() {
        provider.expect(requestTo("http://payments" + PaymentClient.VALIDATE_PATH))
                .andRespond(withSuccess("{\"status\": \"DECLINED\", \"reason\": \"FRAUD_SUSPECTED\"}", MediaType.APPLICATION_JSON));

        PaymentResult result = client.validate(REQUEST, "rsv-10-abc");

        assertThat(result.outcome()).isEqualTo(PaymentOutcome.DECLINED);
        assertThat(result.declineReason()).isEqualTo(DeclineReason.OTHER);
    }

    @Test
    void emptyBodyIsTreatedAsAProviderFailure() {
        provider.expect(requestTo("http://payments" + PaymentClient.VALIDATE_PATH)).andRespond(withSuccess());

        assertThatThrownBy(() -> client.validate(REQUEST, "rsv-10-abc")).isInstanceOf(IllegalStateException.class);
    }
}
