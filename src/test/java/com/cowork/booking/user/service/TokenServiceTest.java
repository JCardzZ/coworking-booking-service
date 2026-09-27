package com.cowork.booking.user.service;

import com.cowork.booking.common.AppConstants.Security;
import com.cowork.booking.config.JwtConfig;
import com.cowork.booking.config.SecurityProperties;
import com.cowork.booking.user.dto.TokenResponse;
import com.cowork.booking.user.model.Role;
import com.cowork.booking.user.model.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private static final SecurityProperties PROPERTIES = new SecurityProperties(
            new SecurityProperties.Jwt("test-only-secret-0123456789abcdef0123", Duration.ofHours(1)),
            new SecurityProperties.Admin("admin@test.com", "Admin123!", "Admin"));

    private final JwtConfig jwtConfig = new JwtConfig();
    private final SecretKey key = invoke("jwtSigningKey", PROPERTIES);
    private final JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    private final JwtDecoder decoder = invoke("jwtDecoder", key);

    @Test
    void issuedTokenCarriesIdentityRoleAndExpiration() {
        User user = user(Role.ADMIN);
        TokenService tokenService = new TokenService(encoder, PROPERTIES, Clock.systemUTC());

        TokenResponse response = tokenService.issue(user);
        Jwt jwt = decoder.decode(response.accessToken());

        assertThat(response.tokenType()).isEqualTo(Security.TOKEN_TYPE);
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString(Security.EMAIL_CLAIM)).isEqualTo("admin@test.com");
        assertThat(jwt.getClaimAsStringList(Security.ROLES_CLAIM)).containsExactly("ADMIN");
        assertThat(jwt.getClaimAsString(JwtClaimNames.ISS)).isEqualTo(Security.ISSUER);
    }

    @Test
    void expiredTokenIsRejected() {
        Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        String token = new TokenService(encoder, PROPERTIES, twoHoursAgo).issue(user(Role.USER)).accessToken();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    private static User user(Role role) {
        User user = new User("admin@test.com", "hash", "Admin", role);
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(String method, Object argument) {
        return (T) ReflectionTestUtils.invokeMethod(jwtConfig, method, argument);
    }
}
