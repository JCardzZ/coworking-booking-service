package com.cowork.booking.reservation.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CreateReservationRequestTest {

    private static final Instant NINE = Instant.parse("2027-01-15T09:00:00Z");
    private static final Instant TEN = Instant.parse("2027-01-15T10:00:00Z");

    @Test
    void endMustBeAfterStart() {
        assertThat(new CreateReservationRequest(1L, NINE, TEN).isValidPeriod()).isTrue();
        assertThat(new CreateReservationRequest(1L, TEN, NINE).isValidPeriod()).isFalse();
        assertThat(new CreateReservationRequest(1L, NINE, NINE).isValidPeriod()).isFalse();
    }

    @Test
    void missingDatesAreLeftToNotNull() {
        assertThat(new CreateReservationRequest(1L, null, TEN).isValidPeriod()).isTrue();
        assertThat(new CreateReservationRequest(1L, NINE, null).isValidPeriod()).isTrue();
    }
}
