package com.cowork.booking.common;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    public ResourceNotFoundException(String resourceType, Object resourceId, String message) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = String.valueOf(resourceId);
    }
}
