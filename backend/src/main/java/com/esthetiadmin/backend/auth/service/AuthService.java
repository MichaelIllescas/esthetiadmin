package com.esthetiadmin.backend.auth.service;

import com.esthetiadmin.backend.auth.dto.LoginRequest;
import com.esthetiadmin.backend.auth.dto.LoginResponse;
import com.esthetiadmin.backend.auth.entity.AppUser;
import com.esthetiadmin.backend.auth.repository.AppUserRepository;
import com.esthetiadmin.backend.auth.security.JwtProperties;
import com.esthetiadmin.backend.common.exception.TooManyRequestsException;
import com.esthetiadmin.backend.common.exception.UnauthorizedException;
import com.esthetiadmin.backend.common.ratelimit.RateLimitConfig;
import com.esthetiadmin.backend.common.ratelimit.RateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Usuario o contraseña incorrectos";
    private static final String TOO_MANY_FAILURES = "Demasiados intentos fallidos. Esperá unos minutos y volvé a intentar";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final RateLimiter failedLogins;
    /** Compared against when the user does not exist, so both failures take the same time. */
    private final String unknownUserHash;

    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder, JwtProperties jwtProperties, Clock clock,
                       @Qualifier(RateLimitConfig.FAILED_LOGIN) RateLimiter failedLogins) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.failedLogins = failedLogins;
        this.unknownUserHash = passwordEncoder.encode("unknown-user-placeholder");
    }

    /**
     * Too many failures from {@code clientIp} block it for a while, even with the right password, so guessing does
     * not pay off (ADR 015). Only failures count.
     */
    public LoginResponse login(LoginRequest request, String clientIp) {
        if (failedLogins.isExhausted(clientIp)) {
            log.warn("Login blocked after too many failures");
            throw new TooManyRequestsException(TOO_MANY_FAILURES);
        }
        AppUser user = appUserRepository.findByUsername(request.username()).orElse(null);
        String hashToCheck = user != null ? user.getPasswordHash() : unknownUserHash;
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (user == null || !passwordMatches) {
            failedLogins.record(clientIp);
            log.warn("Failed login attempt");
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        return issueToken(user);
    }

    private LoginResponse issueToken(AppUser user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(Duration.ofMinutes(jwtProperties.expirationMinutes()));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getUsername())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(token, expiresAt);
    }
}
