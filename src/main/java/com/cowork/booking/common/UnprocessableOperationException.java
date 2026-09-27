package com.cowork.booking.common;

import lombok.Getter;

/** Valid request that a business rule rejects (e.g. an admin disabling their own account). Mapped to 422. */
@Getter
public class UnprocessableOperationException extends RuntimeException {

    private final String code;

    public UnprocessableOperationException(String code, String message) {
        super(message);
        this.code = code;
    }
}
