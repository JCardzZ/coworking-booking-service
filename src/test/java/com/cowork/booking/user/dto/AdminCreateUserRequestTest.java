package com.cowork.booking.user.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminCreateUserRequestTest {

    @Test
    void passwordNeverShowsUpInLogs() {
        String text = new AdminCreateUserRequest("ana@test.com", "Secreta123", "Ana", "USER").toString();

        assertThat(text).contains("ana@test.com").doesNotContain("Secreta123");
    }
}
