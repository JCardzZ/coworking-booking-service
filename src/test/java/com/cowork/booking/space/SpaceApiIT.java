package com.cowork.booking.space;

import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpaceApiIT extends IntegrationTest {

    private static final String SPACES = Api.BASE_PATH + Api.SPACES;

    @Test
    void spaceLifecycle() throws Exception {
        String name = "Sala Norte " + System.nanoTime();
        Long id = idOf(perform(post(SPACES).contentType(MediaType.APPLICATION_JSON).content(space(name, "Piso 9 Norte")), adminToken)
                .andExpect(status().isCreated()));
        String userToken = registerUser();

        perform(get(SPACES).param("type", "MEETING_ROOM").param("minCapacity", "8").param("location", "  norte "), userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItem(name)));
        perform(get(SPACES).param("minCapacity", "50").param("location", " "), userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", not(hasItem(name))));
        perform(get(SPACES + "/{id}", id), userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));

        perform(put(SPACES + "/{id}", id).contentType(MediaType.APPLICATION_JSON).content(space(name + " B", "Piso 10")), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name + " B"));

        // soft delete: it disappears and the name is free again
        perform(delete(SPACES + "/{id}", id), adminToken).andExpect(status().isNoContent());
        perform(get(SPACES + "/{id}", id), userToken).andExpect(status().isNotFound());
        perform(post(SPACES).contentType(MediaType.APPLICATION_JSON).content(space(name + " B", "Piso 10")), adminToken)
                .andExpect(status().isCreated());
    }

    private static String space(String name, String location) {
        return """
                {"name": "%s", "type": "MEETING_ROOM", "capacity": 8, "location": "%s", "hourlyRate": 25}
                """.formatted(name, location);
    }
}
