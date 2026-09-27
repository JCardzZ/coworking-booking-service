package com.cowork.booking.common;

import com.cowork.booking.common.AppConstants.Audit;
import com.cowork.booking.common.AppConstants.Security;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

/** Identity of the authenticated caller, taken only from the validated JWT. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static String email() {
        return jwt().map(token -> token.getToken().getClaimAsString(Security.EMAIL_CLAIM)).orElse(Audit.SYSTEM_USER);
    }

    public static List<String> roles() {
        return jwt().map(token -> token.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(authority -> authority.replaceFirst(Security.ROLE_PREFIX, ""))
                        .toList())
                .orElse(List.of());
    }

    private static Optional<JwtAuthenticationToken> jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof JwtAuthenticationToken token ? Optional.of(token) : Optional.empty();
    }
}
