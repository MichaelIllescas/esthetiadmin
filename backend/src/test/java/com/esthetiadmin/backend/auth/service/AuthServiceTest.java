package com.esthetiadmin.backend.auth.service;

import com.esthetiadmin.backend.auth.dto.LoginRequest;
import com.esthetiadmin.backend.auth.dto.LoginResponse;
import com.esthetiadmin.backend.auth.entity.AppUser;
import com.esthetiadmin.backend.auth.repository.AppUserRepository;
import com.esthetiadmin.backend.auth.security.JwtProperties;
import com.esthetiadmin.backend.common.exception.TooManyRequestsException;
import com.esthetiadmin.backend.common.exception.UnauthorizedException;
import com.esthetiadmin.backend.common.ratelimit.RateLimiter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final String SECRET = "test-secret-with-more-than-thirty-two-characters";
    private static final String IP = "203.0.113.7";
    private static final Instant NOW = Instant.parse("2026-09-25T12:00:00Z");

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private AppUserRepository repository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        repository = mock(AppUserRepository.class);
        authService = new AuthService(
                repository,
                passwordEncoder,
                new NimbusJwtEncoder(new ImmutableSecret<>(key)),
                new JwtProperties(SECRET, 480),
                Clock.fixed(NOW, ZoneOffset.UTC),
                new RateLimiter(2, Duration.ofMinutes(15), Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    @Test
    void validCredentialsReturnTokenWithSubjectAndExpiration() {
        when(repository.findByUsername("admin"))
                .thenReturn(Optional.of(new AppUser("admin", passwordEncoder.encode("correct-password"))));

        LoginResponse response = authService.login(new LoginRequest("admin", "correct-password"), IP);

        Jwt jwt = decoderWithoutExpirationCheck().decode(response.token());
        assertThat(jwt.getSubject()).isEqualTo("admin");
        assertThat(response.expiresAt()).isEqualTo(NOW.plusSeconds(480 * 60L));
        assertThat(jwt.getExpiresAt()).isEqualTo(response.expiresAt());
    }

    @Test
    void wrongPasswordIsRejected() {
        when(repository.findByUsername("admin"))
                .thenReturn(Optional.of(new AppUser("admin", passwordEncoder.encode("correct-password"))));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong-password"), IP))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Usuario o contraseña incorrectos");
    }

    @Test
    void unknownUserGetsTheSameErrorAsWrongPassword() {
        when(repository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "whatever"), IP))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Usuario o contraseña incorrectos");
    }

    @Test
    void tooManyFailuresBlockTheIpEvenWithTheRightPassword() {
        when(repository.findByUsername("admin"))
                .thenReturn(Optional.of(new AppUser("admin", passwordEncoder.encode("correct-password"))));
        for (int attempt = 0; attempt < 2; attempt++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong-password"), IP))
                    .isInstanceOf(UnauthorizedException.class);
        }

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "correct-password"), IP))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageStartingWith("Demasiados intentos fallidos");
        assertThat(authService.login(new LoginRequest("admin", "correct-password"), "198.51.100.1").token()).isNotBlank();
    }

    @Test
    void aSuccessfulLoginDoesNotCount() {
        when(repository.findByUsername("admin"))
                .thenReturn(Optional.of(new AppUser("admin", passwordEncoder.encode("correct-password"))));

        for (int attempt = 0; attempt < 3; attempt++) {
            assertThat(authService.login(new LoginRequest("admin", "correct-password"), IP).token()).isNotBlank();
        }
    }

    /** The fixed clock is in the past, so the expiration check is skipped to be able to inspect the claims. */
    private NimbusJwtDecoder decoderWithoutExpirationCheck() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(token -> OAuth2TokenValidatorResult.success());
        return decoder;
    }
}
