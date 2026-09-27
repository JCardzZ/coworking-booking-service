package com.cowork.booking.common;

import com.cowork.booking.common.AppConstants.Audit;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger AUDIT = LoggerFactory.getLogger(Audit.LOGGER);

    @AfterReturning("@annotation(audited)")
    public void success(Audited audited) {
        AUDIT.info(Audit.LOG_SUCCESS, audited.value(), CurrentUser.email(), CurrentUser.roles());
    }

    @AfterThrowing(pointcut = "@annotation(audited)", throwing = "ex")
    public void failure(Audited audited, Exception ex) {
        AUDIT.warn(Audit.LOG_FAILURE, audited.value(), CurrentUser.email(), CurrentUser.roles(), ex.getClass().getSimpleName());
    }
}
