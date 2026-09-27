package com.cowork.booking.user.service;

import com.cowork.booking.common.AppConstants.Security;
import com.cowork.booking.config.SecurityProperties;
import com.cowork.booking.user.dto.TokenResponse;
import com.cowork.booking.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties securityProperties;
    private final Clock clock;

    public TokenResponse issue(User user) {
        Instant now = clock.instant();
        Duration expiration = securityProperties.jwt().expiration();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(Security.ISSUER)
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .claim(Security.EMAIL_CLAIM, user.getEmail())
                .claim(Security.ROLES_CLAIM, List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, Security.TOKEN_TYPE, expiration.toSeconds());
    }
}
