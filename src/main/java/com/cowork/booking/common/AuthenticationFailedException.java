package com.cowork.booking.common;

import lombok.Getter;

/** Login rejected (wrong credentials or blocked account), mapped to 401. */
@Getter
public class AuthenticationFailedException extends RuntimeException {

    private final String code;

    public AuthenticationFailedException(String code, String message) {
        super(message);
        this.code = code;
    }
}
