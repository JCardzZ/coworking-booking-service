package com.cowork.booking.user.service;

import java.util.Set;

/** Cached snapshot of what a user can do. */
public record UserAuthorization(Long userId, String role, boolean active, Set<String> permissions) {
}
