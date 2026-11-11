package com.esthetiadmin.backend.availability.controller;

import com.esthetiadmin.backend.auth.security.ApiSecurityErrorHandler;
import com.esthetiadmin.backend.auth.security.JwtConfig;
import com.esthetiadmin.backend.auth.security.SecurityConfig;
import com.esthetiadmin.backend.availability.dto.AvailabilityBlockResponse;
import com.esthetiadmin.backend.availability.dto.AvailabilityRuleRequest;
import com.esthetiadmin.backend.availability.dto.AvailabilityRuleResponse;
import com.esthetiadmin.backend.availability.service.AvailabilityService;
import com.esthetiadmin.backend.common.config.CorsConfig;
import com.esthetiadmin.backend.common.exception.BadRequestException;
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
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AvailabilityRuleController.class, AvailabilityBlockController.class})
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, JwtConfig.class, CorsConfig.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-with-more-than-thirty-two-characters",
        "app.security.jwt.expiration-minutes=60",
        "app.cors.allowed-origins=http://localhost:4200"
})
class AvailabilityControllerTest {

    private static final LocalDate HOLIDAY = LocalDate.of(2026, 10, 12);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvailabilityService availabilityService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/availability-rules")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/availability-blocks")).andExpect(status().isUnauthorized());
    }

    @Test
    void listsTheRulesWithHoursAndMinutesOnly() throws Exception {
        when(availabilityService.getRules()).thenReturn(List.of(
                new AvailabilityRuleResponse(1, LocalTime.of(9, 0), LocalTime.of(13, 0))));

        mockMvc.perform(get("/api/availability-rules").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$[0].startTime").value("09:00"))
                .andExpect(jsonPath("$[0].endTime").value("13:00"));
    }

    @Test
    void replacesTheWeekFromAnArray() throws Exception {
        when(availabilityService.replaceRules(anyList())).thenReturn(List.of());

        mockMvc.perform(put("/api/availability-rules").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"dayOfWeek\":1,\"startTime\":\"09:00\",\"endTime\":\"13:00\"}]"))
                .andExpect(status().isOk());

        verify(availabilityService).replaceRules(
                List.of(new AvailabilityRuleRequest(1, LocalTime.of(9, 0), LocalTime.of(13, 0))));
    }

    @Test
    void anInvalidWeekReturns400WithTheReason() throws Exception {
        when(availabilityService.replaceRules(anyList()))
                .thenThrow(new BadRequestException("Los rangos del lunes se superponen"));

        mockMvc.perform(put("/api/availability-rules").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Los rangos del lunes se superponen"));
    }

    @Test
    void createsAWholeDayBlock() throws Exception {
        when(availabilityService.createBlock(any())).thenReturn(
                new AvailabilityBlockResponse(3L, HOLIDAY, HOLIDAY, null, null, "Feriado", List.of()));

        mockMvc.perform(post("/api/availability-blocks").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2026-10-12\",\"endDate\":\"2026-10-12\",\"startTime\":null,\"endTime\":null,\"reason\":\"Feriado\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.endDate").value("2026-10-12"))
                .andExpect(jsonPath("$.startTime").isEmpty())
                .andExpect(jsonPath("$.affectedAppointments").isArray());
    }

    @Test
    void aBlockWithoutDateReturns400() throws Exception {
        mockMvc.perform(post("/api/availability-blocks").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Feriado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("La fecha de inicio del bloqueo es obligatoria")));
    }

    @Test
    void listsBlocksOfARange() throws Exception {
        when(availabilityService.listBlocks(HOLIDAY, HOLIDAY.plusDays(7))).thenReturn(List.of(
                new AvailabilityBlockResponse(3L, HOLIDAY, HOLIDAY, LocalTime.of(15, 0), LocalTime.of(19, 0), null,
                        List.of())));

        mockMvc.perform(get("/api/availability-blocks").with(jwt())
                        .param("from", "2026-10-12").param("to", "2026-10-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startTime").value("15:00"));
    }

    @Test
    void deletesABlock() throws Exception {
        mockMvc.perform(delete("/api/availability-blocks/3").with(jwt())).andExpect(status().isNoContent());

        verify(availabilityService).deleteBlock(3L);
    }
}
