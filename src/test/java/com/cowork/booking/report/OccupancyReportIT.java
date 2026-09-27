package com.cowork.booking.report;

import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.common.AppConstants.Caches;
import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.report.dto.OccupancyReportRequest;
import com.cowork.booking.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OccupancyReportIT extends IntegrationTest {

    private static final String OCCUPANCY = Api.BASE_PATH + Api.REPORTS + Api.OCCUPANCY;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void reportCountsConfirmedHoursInsideTheRangeAndRefreshesOnChanges() throws Exception {
        LocalDate day = LocalDate.now(ZoneOffset.UTC).plusDays(10);
        Instant midnight = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        Long spaceId = createSpace(20);
        Long morning = createReservation(spaceId, at(midnight, 9), at(midnight, 12));
        // starts the day before: only its 2 hours inside the day count
        Long overnight = createReservation(spaceId, at(midnight, -2), at(midnight, 2));
        Long afternoon = createReservation(spaceId, at(midnight, 14), at(midnight, 15));
        confirm(morning).andExpect(status().isOk());
        confirm(overnight).andExpect(status().isOk());

        // afternoon is still pending payment, so it doesn't count yet
        report(spaceId, day, day)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spaces[0].confirmedReservations").value(2))
                .andExpect(jsonPath("$.spaces[0].reservedHours").value(5.0))
                .andExpect(jsonPath("$.spaces[0].availableHours").value(24))
                .andExpect(jsonPath("$.spaces[0].occupancyPercent").value(20.83));
        assertThat(cacheManager.getCache(Caches.OCCUPANCY_REPORT).get(new OccupancyReportRequest(day, day, spaceId)))
                .isNotNull();

        confirm(afternoon).andExpect(status().isOk());
        report(spaceId, day, day).andExpect(jsonPath("$.spaces[0].reservedHours").value(6.0));

        cancel(morning).andExpect(status().isOk());
        report(spaceId, day, day)
                .andExpect(jsonPath("$.spaces[0].confirmedReservations").value(2))
                .andExpect(jsonPath("$.spaces[0].reservedHours").value(3.0))
                .andExpect(jsonPath("$.spaces[0].occupancyPercent").value(12.5));
    }

    @Test
    void invalidRangeIsRejected() throws Exception {
        LocalDate day = LocalDate.now(ZoneOffset.UTC);

        report(null, day, day.minusDays(1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR));
        report(null, day, day.plusDays(400)).andExpect(status().isBadRequest());
    }

    @Test
    void userWithoutReportPermissionIsForbidden() throws Exception {
        LocalDate day = LocalDate.now(ZoneOffset.UTC);

        mockMvc.perform(get(OCCUPANCY).param("from", day.toString()).param("to", day.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(registerUser())))
                .andExpect(status().isForbidden());
    }

    private ResultActions report(Long spaceId, LocalDate from, LocalDate to) throws Exception {
        var request = get(OCCUPANCY).param("from", from.toString()).param("to", to.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken));
        if (spaceId != null) {
            request.param("spaceId", spaceId.toString());
        }
        return mockMvc.perform(request);
    }

    private static Instant at(Instant midnight, int hours) {
        return midnight.plus(hours, ChronoUnit.HOURS);
    }
}
