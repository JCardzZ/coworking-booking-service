package com.cowork.booking.reservation;

import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.reservation.dto.CreateReservationRequest;
import com.cowork.booking.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationQueryIT extends IntegrationTest {

    @Test
    void eachUserSeesTheirOwnAndTheAdminSeesEveryone() throws Exception {
        Long spaceId = createSpace(20);
        Instant start = Instant.now().plus(6, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        String userToken = registerUser();

        String key = "same-key-" + System.nanoTime();
        String body = objectMapper.writeValueAsString(new CreateReservationRequest(spaceId, start, start.plus(1, ChronoUnit.HOURS)));
        Long mine = idOf(perform(post(RESERVATIONS).header(Api.IDEMPOTENCY_KEY_HEADER, key)
                .contentType(MediaType.APPLICATION_JSON).content(body), userToken).andExpect(status().isCreated()));
        // retrying with the same key gives back the same reservation
        perform(post(RESERVATIONS).header(Api.IDEMPOTENCY_KEY_HEADER, key)
                        .contentType(MediaType.APPLICATION_JSON).content(body), userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mine));

        Long others = createReservation(spaceId, start.plus(2, ChronoUnit.HOURS), start.plus(3, ChronoUnit.HOURS));

        perform(get(RESERVATIONS).param("spaceId", spaceId.toString()).param("status", "PENDING_PAYMENT")
                        .param("from", start.toString()).param("to", start.plus(1, ChronoUnit.DAYS).toString()), userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(mine));
        perform(get(RESERVATIONS).param("spaceId", spaceId.toString()), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        String userId = objectMapper.readTree(perform(get(RESERVATIONS + "/{id}", mine), userToken)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("userId").asText();
        perform(get(RESERVATIONS).param("userId", userId), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].userId", everyItem(is(Integer.valueOf(userId)))))
                .andExpect(jsonPath("$.content[*].id", hasItem(mine.intValue())));

        // someone else's reservation looks like it doesn't exist
        perform(get(RESERVATIONS + "/{id}", others), userToken).andExpect(status().isNotFound());
        perform(get(RESERVATIONS + "/{id}" + Api.PAYMENTS, others), userToken).andExpect(status().isNotFound());
        perform(get(RESERVATIONS + "/{id}", mine), adminToken).andExpect(status().isOk());
        perform(get(RESERVATIONS + "/999999"), adminToken).andExpect(status().isNotFound());
    }
}
