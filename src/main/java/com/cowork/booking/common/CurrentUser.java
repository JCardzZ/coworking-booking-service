package com.cowork.booking.common;

import com.cowork.booking.common.AppConstants.Audit;
import com.cowork.booking.common.AppConstants.Security;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

/** Current caller, read from the validated JWT. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<Long> id() {
        return jwt().map(token -> Long.valueOf(token.getToken().getSubject()));
    }

    public static String email() {
        return jwt().map(token -> token.getToken().getClaimAsString(Security.EMAIL_CLAIM)).orElse(Audit.SYSTEM_USER);
    }

    // Comes from the DB-backed authorities, so it reflects role changes
    public static String role() {
        return jwt().flatMap(token -> token.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(authority -> authority.startsWith(Security.ROLE_PREFIX))
                        .map(authority -> authority.substring(Security.ROLE_PREFIX.length()))
                        .findFirst())
                .orElse(Audit.SYSTEM_USER);
    }

    public static boolean hasAuthority(String authority) {
        return jwt().map(token -> token.getAuthorities().stream()
                        .anyMatch(granted -> granted.getAuthority().equals(authority)))
                .orElse(false);
    }

    private static Optional<JwtAuthenticationToken> jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof JwtAuthenticationToken token ? Optional.of(token) : Optional.empty();
    }
}
