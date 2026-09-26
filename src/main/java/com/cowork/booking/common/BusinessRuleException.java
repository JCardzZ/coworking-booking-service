package com.cowork.booking.common;

import lombok.Getter;
import org.springframework.lang.Nullable;

/** Business rule violation, mapped to 409. */
@Getter
public class BusinessRuleException extends RuntimeException {

    private final String code;
    @Nullable
    private final String conflictingField;

    public BusinessRuleException(String code, String message) {
        this(code, message, null);
    }

    public BusinessRuleException(String code, String message, @Nullable String conflictingField) {
        super(message);
        this.code = code;
        this.conflictingField = conflictingField;
    }
}
