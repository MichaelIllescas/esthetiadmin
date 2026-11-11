package com.esthetiadmin.backend.client.controller;

import com.esthetiadmin.backend.auth.security.ApiSecurityErrorHandler;
import com.esthetiadmin.backend.auth.security.JwtConfig;
import com.esthetiadmin.backend.auth.security.SecurityConfig;
import com.esthetiadmin.backend.client.dto.BirthdayResponse;
import com.esthetiadmin.backend.client.dto.ClientResponse;
import com.esthetiadmin.backend.client.service.ClientService;
import com.esthetiadmin.backend.common.config.CorsConfig;
import com.esthetiadmin.backend.common.dto.PageResponse;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, JwtConfig.class, CorsConfig.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-with-more-than-thirty-two-characters",
        "app.security.jwt.expiration-minutes=60",
        "app.cors.allowed-origins=http://localhost:4200"
})
class ClientControllerTest {

    private static final String VALID_BODY =
            "{\"firstName\":\"Carina\",\"lastName\":\"Diaz\",\"phone\":\"3415551234\",\"birthDate\":\"1985-11-03\",\"notes\":null}";

    private static final ClientResponse CARINA =
            new ClientResponse(7L, "Carina", "Diaz", "3415551234", LocalDate.of(1985, 11, 3), 40, null, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/clients")).andExpect(status().isUnauthorized());
    }

    @Test
    void searchReturnsAPage() throws Exception {
        when(clientService.search("cari", 0, 20)).thenReturn(new PageResponse<>(List.of(CARINA), 0, 20, 1, 1));

        mockMvc.perform(get("/api/clients").param("search", "cari").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("Carina"))
                .andExpect(jsonPath("$.content[0].age").value(40))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void searchRejectsAnOversizedPage() throws Exception {
        mockMvc.perform(get("/api/clients").param("size", "500").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("El tamaño de página no puede superar 100"));
    }

    @Test
    void getReturnsTheClient() throws Exception {
        when(clientService.get(7L)).thenReturn(CARINA);

        mockMvc.perform(get("/api/clients/7").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Diaz"))
                .andExpect(jsonPath("$.birthDate").value("1985-11-03"));
    }

    @Test
    void createReturns201() throws Exception {
        when(clientService.create(any())).thenReturn(CARINA);

        mockMvc.perform(post("/api/clients").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void createWithoutNamesReturns400WithBothMessages() throws Exception {
        mockMvc.perform(post("/api/clients").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"\",\"lastName\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("El nombre es obligatorio")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("El apellido es obligatorio")));
    }

    @Test
    void createWithAFutureBirthDateReturns400() throws Exception {
        mockMvc.perform(post("/api/clients").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ana\",\"lastName\":\"Lopez\",\"birthDate\":\"2999-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha de nacimiento no puede ser futura"));
    }

    @Test
    void updateReturns200() throws Exception {
        when(clientService.update(eq(7L), any())).thenReturn(CARINA);

        mockMvc.perform(put("/api/clients/7").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Carina"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/clients/7").with(jwt())).andExpect(status().isNoContent());

        verify(clientService).delete(7L);
    }

    @Test
    void deleteOfAClientWithTreatmentsReturns409() throws Exception {
        doThrow(new ConflictException("El cliente tiene tratamientos registrados. Eliminá esos tratamientos primero"))
                .when(clientService).delete(7L);

        mockMvc.perform(delete("/api/clients/7").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void existsAnswersWhetherThereIsAHomonym() throws Exception {
        when(clientService.existsWithName("Carina", "Diaz", null)).thenReturn(true);

        mockMvc.perform(get("/api/clients/exists").param("firstName", "Carina").param("lastName", "Diaz").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    void existsCanExcludeTheClientBeingEdited() throws Exception {
        when(clientService.existsWithName("Carina", "Diaz", 7L)).thenReturn(false);

        mockMvc.perform(get("/api/clients/exists")
                        .param("firstName", "Carina").param("lastName", "Diaz").param("excludeId", "7").with(jwt()))
                .andExpect(jsonPath("$.exists").value(false));
    }

    @Test
    void birthdaysDefaultToThirtyDays() throws Exception {
        when(clientService.upcomingBirthdays(30)).thenReturn(List.of(
                new BirthdayResponse(7L, "Carina", "Diaz", LocalDate.of(1985, 11, 3), LocalDate.of(2026, 11, 3), 41, 39)));

        mockMvc.perform(get("/api/clients/birthdays").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].turningAge").value(41))
                .andExpect(jsonPath("$[0].daysUntil").value(39))
                .andExpect(jsonPath("$[0].nextBirthday").value("2026-11-03"));
    }

    @Test
    void birthdaysRejectAnInvalidWindow() throws Exception {
        mockMvc.perform(get("/api/clients/birthdays").param("days", "0").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Los días deben ser al menos 1"));
    }
}
