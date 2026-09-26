package com.cowork.booking.config;

import com.cowork.booking.CoworkingBookingServiceApplication;
import com.cowork.booking.common.AppConstants.Api;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Prefixes our controllers with {@code /api/v1}; springdoc and actuator are excluded. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(Api.BASE_PATH, HandlerTypePredicate
                .forBasePackageClass(CoworkingBookingServiceApplication.class)
                .and(HandlerTypePredicate.forAnnotation(RestController.class)));
    }
}
