package com.cowork.booking.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Writes an audit log line (action, user, roles, outcome) for the annotated service method. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** Action name from {@link AppConstants.Audit}. */
    String value();
}
