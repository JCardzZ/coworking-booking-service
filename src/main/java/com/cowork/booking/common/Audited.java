package com.cowork.booking.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Logs an audit line (action, user, role, outcome) for the annotated method. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** Action name from {@link AppConstants.Audit}. */
    String value();
}
