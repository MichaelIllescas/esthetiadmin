package com.esthetiadmin.backend.treatment.controller;

import com.esthetiadmin.backend.auth.security.ApiSecurityErrorHandler;
import com.esthetiadmin.backend.auth.security.JwtConfig;
import com.esthetiadmin.backend.auth.security.SecurityConfig;
import com.esthetiadmin.backend.common.config.CorsConfig;
import com.esthetiadmin.backend.common.dto.PageResponse;
import com.esthetiadmin.backend.common.exception.BadRequestException;
import com.esthetiadmin.backend.common.exception.ConflictException;
import com.esthetiadmin.backend.common.exception.GlobalExceptionHandler;
import com.esthetiadmin.backend.treatment.dto.TreatmentResponse;
import com.esthetiadmin.backend.treatment.service.TreatmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TreatmentController.class)
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, JwtConfig.class, CorsConfig.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-with-more-than-thirty-two-characters",
        "app.security.jwt.expiration-minutes=60",
        "app.cors.allowed-origins=http://localhost:4200"
})
class TreatmentControllerTest {

    private static final TreatmentResponse BOZO = new TreatmentResponse(31L, 7L, "Diaz, Carina", 10L, "Bozo", 1L,
            "Depilación láser", LocalDate.of(2026, 9, 25), new BigDecimal("15000.00"), null);

    private static String body(String amount, String date) {
        return "{\"clientId\":7,\"serviceId\":10,\"performedOn\":\"" + date + "\",\"amount\":" + amount + ",\"notes\":null}";
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TreatmentService treatmentService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/treatments")).andExpect(status().isUnauthorized());
    }

    @Test
    void createReturns201() throws Exception {
        when(treatmentService.create(any())).thenReturn(BOZO);

        mockMvc.perform(post("/api/treatments").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(body("15000.00", "2026-09-25")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.serviceName").value("Bozo"))
                .andExpect(jsonPath("$.clientName").value("Diaz, Carina"));
    }

    @Test
    void createAcceptsAFreeTreatment() throws Exception {
        when(treatmentService.create(any())).thenReturn(BOZO);

        mockMvc.perform(post("/api/treatments").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(body("0", "2026-09-25")))
                .andExpect(status().isCreated());
    }

    @Test
    void createWithoutTheRequiredFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/treatments").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("El cliente es obligatorio")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("El servicio es obligatorio")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("La fecha es obligatoria")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("El importe es obligatorio")));
    }

    @Test
    void createWithAFutureDateReturns400() throws Exception {
        mockMvc.perform(post("/api/treatments").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(body("100", "2999-01-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha no puede ser futura"));
    }

    @Test
    void createWithANegativeAmountReturns400() throws Exception {
        mockMvc.perform(post("/api/treatments").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(body("-1", "2026-09-25")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El importe no puede ser negativo"));
    }

    @Test
    void createWithAnUnavailableServiceReturns409() throws Exception {
        when(treatmentService.create(any())).thenThrow(new ConflictException("El servicio no está disponible: está desactivado o su categoría está inactiva"));

        mockMvc.perform(post("/api/treatments").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(body("100", "2026-09-25")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void updateReturns200AndDoesNotAskForTheClient() throws Exception {
        when(treatmentService.update(eq(31L), any())).thenReturn(BOZO);

        mockMvc.perform(put("/api/treatments/31").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\":10,\"performedOn\":\"2026-09-25\",\"amount\":12000,\"notes\":\"Con descuento\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(31));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/treatments/31").with(jwt())).andExpect(status().isNoContent());

        verify(treatmentService).delete(31L);
    }

    @Test
    void searchPassesEveryFilterToTheService() throws Exception {
        when(treatmentService.search(7L, 10L, 1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), 2, 10))
                .thenReturn(new PageResponse<>(List.of(BOZO), 2, 10, 21, 3));

        mockMvc.perform(get("/api/treatments").with(jwt())
                        .param("clientId", "7").param("serviceId", "10").param("categoryId", "1")
                        .param("from", "2026-09-01").param("to", "2026-09-30")
                        .param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categoryName").value("Depilación láser"))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void searchWithoutFiltersUsesTheDefaults() throws Exception {
        when(treatmentService.search(null, null, null, null, null, 0, 20))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/treatments").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void searchWithAMalformedDateReturns400() throws Exception {
        mockMvc.perform(get("/api/treatments").with(jwt()).param("from", "25/09/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El valor del parámetro 'from' no es válido"));
    }

    @Test
    void searchWithAnInvertedRangeReturns400() throws Exception {
        when(treatmentService.search(any(), any(), any(), any(), any(), eq(0), eq(20)))
                .thenThrow(new BadRequestException("La fecha \"desde\" no puede ser posterior a la fecha \"hasta\""));

        mockMvc.perform(get("/api/treatments").with(jwt()).param("from", "2026-09-30").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void searchRejectsAnOversizedPage() throws Exception {
        mockMvc.perform(get("/api/treatments").with(jwt()).param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El tamaño de página no puede superar 100"));
    }
}
