package com.cowork.booking.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailsTest {

    @Test
    void onlyProblemsWithOurCodeCountAsOurs() {
        ProblemDetail bare = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        assertThat(ProblemDetails.hasCode(bare)).isFalse();

        bare.setProperty("other", "value");
        assertThat(ProblemDetails.hasCode(bare)).isFalse();

        bare.setProperty("code", "SOMETHING");
        assertThat(ProblemDetails.hasCode(bare)).isTrue();
    }
}
