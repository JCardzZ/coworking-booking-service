package com.cowork.booking.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Map;

/** Pool size and shutdown come from spring.task.execution in application.yml. */
@Configuration
@EnableAsync
public class AsyncConfig {

    // Boot applies it to the async executor: keeps the correlation id in the logs of async tasks
    @Bean
    TaskDecorator mdcTaskDecorator() {
        return task -> {
            Map<String, String> context = MDC.getCopyOfContextMap();
            return () -> {
                if (context != null) {
                    MDC.setContextMap(context);
                }
                try {
                    task.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }
}
