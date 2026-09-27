package com.cowork.booking.reservation;

import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationConcurrencyIT extends IntegrationTest {

    private static final int PARALLEL_REQUESTS = 20;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void onlyOneOfManySimultaneousBookingsForTheSameSlotWins() throws Exception {
        Long spaceId = createSpace(20);
        Instant startAt = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant endAt = startAt.plus(2, ChronoUnit.HOURS);

        List<MockHttpServletResponse> responses = runAtTheSameTime(PARALLEL_REQUESTS,
                () -> reserve(adminToken, spaceId, startAt, endAt).andReturn().getResponse());

        assertThat(responses).filteredOn(r -> r.getStatus() == HttpStatus.CREATED.value()).hasSize(1);
        List<MockHttpServletResponse> rejected = responses.stream()
                .filter(r -> r.getStatus() != HttpStatus.CREATED.value()).toList();
        assertThat(rejected).hasSize(PARALLEL_REQUESTS - 1)
                .allSatisfy(r -> {
                    assertThat(r.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                    assertThat(r.getContentAsString()).contains(ErrorCodes.RESERVATION_OVERLAP);
                });
        assertThat(reservationsIn(spaceId)).isEqualTo(1);
    }

    @Test
    void simultaneousBackToBackBookingsBothSucceed() throws Exception {
        Long spaceId = createSpace(20);
        Instant nine = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant ten = nine.plus(1, ChronoUnit.HOURS);
        Instant eleven = ten.plus(1, ChronoUnit.HOURS);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<MockHttpServletResponse> first = executor.submit(
                    whenReleased(start, () -> reserve(adminToken, spaceId, nine, ten).andReturn().getResponse()));
            Future<MockHttpServletResponse> second = executor.submit(
                    whenReleased(start, () -> reserve(adminToken, spaceId, ten, eleven).andReturn().getResponse()));
            start.countDown();

            // end is exclusive: 9-10 and 10-11 don't overlap
            assertThat(first.get().getStatus()).isEqualTo(HttpStatus.CREATED.value());
            assertThat(second.get().getStatus()).isEqualTo(HttpStatus.CREATED.value());
        }
        assertThat(reservationsIn(spaceId)).isEqualTo(2);
    }

    // every task waits on the same latch so they hit the API together, not one after another
    private static List<MockHttpServletResponse> runAtTheSameTime(int count, Callable<MockHttpServletResponse> task)
            throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(count)) {
            List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                futures.add(executor.submit(whenReleased(start, task)));
            }
            start.countDown();
            List<MockHttpServletResponse> responses = new ArrayList<>();
            for (Future<MockHttpServletResponse> future : futures) {
                responses.add(future.get());
            }
            return responses;
        }
    }

    private static Callable<MockHttpServletResponse> whenReleased(CountDownLatch start,
                                                                  Callable<MockHttpServletResponse> task) {
        return () -> {
            start.await();
            return task.call();
        };
    }

    private int reservationsIn(Long spaceId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservations WHERE space_id = ?", Integer.class, spaceId);
    }
}
