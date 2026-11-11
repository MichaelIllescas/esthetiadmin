package com.esthetiadmin.backend.appointment.controller;

import com.esthetiadmin.backend.appointment.dto.AvailableDaysResponse;
import com.esthetiadmin.backend.appointment.dto.PublicBookingResponse;
import com.esthetiadmin.backend.appointment.service.PublicBookingService;
import com.esthetiadmin.backend.auth.security.ApiSecurityErrorHandler;
import com.esthetiadmin.backend.auth.security.JwtConfig;
import com.esthetiadmin.backend.auth.security.SecurityConfig;
import com.esthetiadmin.backend.catalog.dto.BookableCategory;
import com.esthetiadmin.backend.catalog.dto.BookableService;
import com.esthetiadmin.backend.common.config.CorsConfig;
import com.esthetiadmin.backend.common.exception.GlobalExceptionHandler;
import com.esthetiadmin.backend.common.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every request here goes without a token: the public page has no login. */
@WebMvcTest(PublicBookingController.class)
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, JwtConfig.class, CorsConfig.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-with-more-than-thirty-two-characters",
        "app.security.jwt.expiration-minutes=60",
        "app.cors.allowed-origins=http://localhost:4200"
})
class PublicBookingControllerTest {

    private static final String BODY = "{\"serviceId\":10,\"startsAt\":\"2026-10-08T10:00:00\",\"firstName\":\"Carina\","
            + "\"lastName\":\"Diaz\",\"phone\":\"3415551234\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicBookingService bookingService;

    @Test
    void listsTheBookableServicesWithoutPrices() throws Exception {
        when(bookingService.services()).thenReturn(List.of(new BookableCategory(1L, "Depilación", 30,
                List.of(new BookableService(10L, "Bozo", null)))));

        mockMvc.perform(get("/api/public/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].durationMinutes").value(30))
                .andExpect(jsonPath("$[0].services[0].name").value("Bozo"))
                .andExpect(jsonPath("$[0].services[0].price").doesNotExist());
    }

    @Test
    void listsTheDaysOfAMonth() throws Exception {
        when(bookingService.availableDays(10L, YearMonth.of(2026, 10)))
                .thenReturn(new AvailableDaysResponse(YearMonth.of(2026, 10), List.of(LocalDate.of(2026, 10, 8))));

        mockMvc.perform(get("/api/public/availability/days").param("serviceId", "10").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-10"))
                .andExpect(jsonPath("$.days[0]").value("2026-10-08"));
    }

    @Test
    void booksWithTheCallerAddress() throws Exception {
        when(bookingService.book(any(), eq("203.0.113.7"))).thenReturn(new PublicBookingResponse("Bozo",
                LocalDateTime.of(2026, 10, 8, 10, 0), LocalDateTime.of(2026, 10, 8, 10, 30), "Carina", false));

        mockMvc.perform(post("/api/public/appointments").contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.7");
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startsAt").value("2026-10-08T10:00:00"))
                .andExpect(jsonPath("$.id").doesNotExist());
        verify(bookingService).book(any(), eq("203.0.113.7"));
    }

    @Test
    void tooManyRequestsReturns429() throws Exception {
        when(bookingService.book(any(), any())).thenThrow(new TooManyRequestsException("Hiciste demasiados pedidos"));

        mockMvc.perform(post("/api/public/appointments").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void aBookingWithoutNameReturns400() throws Exception {
        mockMvc.perform(post("/api/public/appointments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\":10,\"startsAt\":\"2026-10-08T10:00:00\",\"lastName\":\"Diaz\",\"phone\":\"3415551234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El nombre es obligatorio"));
    }

    @Test
    void theRestOfTheApiStillNeedsLogin() throws Exception {
        mockMvc.perform(get("/api/appointments").param("from", "2026-10-01").param("to", "2026-10-02"))
                .andExpect(status().isUnauthorized());
    }
}
