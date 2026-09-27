package com.cowork.booking.config;

import com.cowork.booking.common.AppConstants.Tracing;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    private final TaskDecorator decorator = new AsyncConfig().mdcTaskDecorator();

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void asyncTaskKeepsTheCorrelationIdOfTheRequest() throws InterruptedException {
        MDC.put(Tracing.MDC_KEY, "abc-123");
        AtomicReference<String> seen = new AtomicReference<>();
        Runnable task = decorator.decorate(() -> seen.set(MDC.get(Tracing.MDC_KEY)));

        Thread thread = new Thread(task);
        thread.start();
        thread.join();

        assertThat(seen.get()).isEqualTo("abc-123");
    }

    @Test
    void worksWhenThereIsNoCorrelationId() {
        AtomicReference<String> seen = new AtomicReference<>("x");
        decorator.decorate(() -> seen.set(MDC.get(Tracing.MDC_KEY))).run();

        assertThat(seen.get()).isNull();
    }
}
