package com.esthetiadmin.backend.catalog.controller;

import com.esthetiadmin.backend.auth.security.ApiSecurityErrorHandler;
import com.esthetiadmin.backend.auth.security.JwtConfig;
import com.esthetiadmin.backend.auth.security.SecurityConfig;
import com.esthetiadmin.backend.catalog.dto.OfferingResponse;
import com.esthetiadmin.backend.catalog.service.OfferingService;
import com.esthetiadmin.backend.common.config.CorsConfig;
import com.esthetiadmin.backend.common.exception.ConflictException;
import com.esthetiadmin.backend.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OfferingController.class)
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, JwtConfig.class, CorsConfig.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-with-more-than-thirty-two-characters",
        "app.security.jwt.expiration-minutes=60",
        "app.cors.allowed-origins=http://localhost:4200"
})
class OfferingControllerTest {

    private static final String VALID_BODY = "{\"categoryId\":1,\"name\":\"Bozo\",\"description\":null,\"price\":15000.00}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OfferingService offeringService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReturns201() throws Exception {
        when(offeringService.create(any()))
                .thenReturn(new OfferingResponse(10L, 1L, "Bozo", null, new BigDecimal("15000.00"), true, true));

        mockMvc.perform(post("/api/services").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.price").value(15000.00));
    }

    @Test
    void createWithoutRequiredFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/services").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createWithANegativePriceReturns400() throws Exception {
        mockMvc.perform(post("/api/services").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":1,\"name\":\"Bozo\",\"price\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El precio no puede ser negativo"));
    }

    @Test
    void createWithARepeatedNameReturns409() throws Exception {
        when(offeringService.create(any()))
                .thenThrow(new ConflictException("Ya existe un servicio con ese nombre en esta categoría"));

        mockMvc.perform(post("/api/services").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void updateReturns200() throws Exception {
        when(offeringService.update(any(), any()))
                .thenReturn(new OfferingResponse(10L, 1L, "Bozo", null, new BigDecimal("18000.00"), true, true));

        mockMvc.perform(put("/api/services/10").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(18000.00));
    }

    @Test
    void setActiveReturns200() throws Exception {
        when(offeringService.setActive(10L, false))
                .thenReturn(new OfferingResponse(10L, 1L, "Bozo", null, new BigDecimal("15000.00"), false, true));

        mockMvc.perform(patch("/api/services/10/active").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/services/10").with(jwt())).andExpect(status().isNoContent());

        verify(offeringService).delete(10L);
    }

    @Test
    void deleteOfAServiceWithTreatmentsReturns409() throws Exception {
        doThrow(new ConflictException("El servicio tiene tratamientos registrados. Desactivalo para que deje de ofrecerse"))
                .when(offeringService).delete(10L);

        mockMvc.perform(delete("/api/services/10").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }
}
