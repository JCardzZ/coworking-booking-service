package com.cowork.booking.common;

/** Wrong credentials on login, mapped to 401. */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
