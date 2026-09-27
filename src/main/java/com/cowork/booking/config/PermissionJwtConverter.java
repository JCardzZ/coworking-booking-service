package com.cowork.booking.config;

import com.cowork.booking.common.AppConstants.Messages;
import com.cowork.booking.common.AppConstants.Security;
import com.cowork.booking.user.service.AuthorizationService;
import com.cowork.booking.user.service.UserAuthorization;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

/** Loads the user's current permissions on each request; blocked users get a 401. */
@Component
@RequiredArgsConstructor
public class PermissionJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AuthorizationService authorizationService;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UserAuthorization authorization = authorizationService.load(Long.valueOf(jwt.getSubject()))
                .filter(UserAuthorization::active)
                .orElseThrow(() -> new DisabledException(Messages.Auth.TOKEN_USER_INACTIVE));
        List<GrantedAuthority> authorities = Stream.concat(
                        authorization.permissions().stream(),
                        Stream.of(Security.ROLE_PREFIX + authorization.role()))
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
