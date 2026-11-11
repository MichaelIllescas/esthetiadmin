package com.esthetiadmin.backend.auth.security;

import com.esthetiadmin.backend.common.config.CorsConfig;
import com.esthetiadmin.backend.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfigTest.ProbeController.class)
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, JwtConfig.class, CorsConfig.class,
        GlobalExceptionHandler.class, SecurityConfigTest.ProbeController.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-with-more-than-thirty-two-characters",
        "app.security.jwt.expiration-minutes=60",
        "app.cors.allowed-origins=http://localhost:4200"
})
class SecurityConfigTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:4200";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void requestWithoutTokenReturns401InApiFormat() throws Exception {
        mockMvc.perform(get("/api/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/probe"));
    }

    @Test
    void requestWithValidTokenIsAllowed() throws Exception {
        mockMvc.perform(get("/api/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(Instant.now().plusSeconds(600))))
                .andExpect(status().isOk());
    }

    @Test
    void requestWithExpiredTokenReturns401() throws Exception {
        String expired = token(Instant.now().minusSeconds(600));

        mockMvc.perform(get("/api/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void requestWithGarbageTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/probe").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRouteDoesNotRequireAuthentication() throws Exception {
        int status = mockMvc.perform(post("/api/auth/login")).andReturn().getResponse().getStatus();

        assertThat(status).isNotEqualTo(401);
    }

    @Test
    void preflightFromAllowedOriginIsAccepted() throws Exception {
        mockMvc.perform(options("/api/probe")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    void preflightAcceptsEveryMethodTheApiUses() throws Exception {
        for (String method : new String[] {"GET", "POST", "PUT", "PATCH", "DELETE"}) {
            mockMvc.perform(options("/api/probe")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
        }
    }

    @Test
    void preflightFromOtherOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/probe")
                        .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private String token(Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("responsable")
                .issuedAt(Instant.now().minusSeconds(3600))
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @RestController
    static class ProbeController {

        @GetMapping("/api/probe")
        String probe() {
            return "ok";
        }
    }
}
