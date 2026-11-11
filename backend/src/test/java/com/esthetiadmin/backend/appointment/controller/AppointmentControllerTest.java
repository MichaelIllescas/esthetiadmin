package com.esthetiadmin.backend.appointment.controller;

import com.esthetiadmin.backend.appointment.dto.AppointmentResponse;
import com.esthetiadmin.backend.appointment.dto.DayAvailabilityResponse;
import com.esthetiadmin.backend.appointment.entity.AppointmentSource;
import com.esthetiadmin.backend.appointment.entity.AppointmentStatus;
import com.esthetiadmin.backend.appointment.service.AppointmentService;
import com.esthetiadmin.backend.auth.security.ApiSecurityErrorHandler;
import com.esthetiadmin.backend.auth.security.JwtConfig;
import com.esthetiadmin.backend.auth.security.SecurityConfig;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, JwtConfig.class, CorsConfig.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-with-more-than-thirty-two-characters",
        "app.security.jwt.expiration-minutes=60",
        "app.cors.allowed-origins=http://localhost:4200"
})
class AppointmentControllerTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 8, 10, 0);
    private static final AppointmentResponse APPOINTMENT = new AppointmentResponse(52L, 10L, "Bozo", "Depilación",
            START, START.plusMinutes(30), "Carina", "Diaz", "3415551234", null, AppointmentStatus.CONFIRMED,
            AppointmentSource.ADMIN, null, null);
    private static final String BODY = "{\"serviceId\":10,\"startsAt\":\"2026-10-08T10:00:00\",\"firstName\":\"Carina\","
            + "\"lastName\":\"Diaz\",\"phone\":\"3415551234\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/appointments").param("from", "2026-10-01").param("to", "2026-10-07"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsTheAgendaWithFullTimestamps() throws Exception {
        when(appointmentService.search(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 8), null))
                .thenReturn(List.of(APPOINTMENT));

        mockMvc.perform(get("/api/appointments").with(jwt()).param("from", "2026-10-08").param("to", "2026-10-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startsAt").value("2026-10-08T10:00:00"))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].source").value("ADMIN"));
    }

    @Test
    void theRangeIsRequired() throws Exception {
        mockMvc.perform(get("/api/appointments").with(jwt())).andExpect(status().isBadRequest());
    }

    @Test
    void createsAnAppointment() throws Exception {
        when(appointmentService.create(any())).thenReturn(APPOINTMENT);

        mockMvc.perform(post("/api/appointments").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(52));
    }

    @Test
    void aTakenTimeReturns409() throws Exception {
        when(appointmentService.create(any())).thenThrow(new ConflictException("Ese horario ya está ocupado"));

        mockMvc.perform(post("/api/appointments").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ese horario ya está ocupado"));
    }

    @Test
    void anAppointmentWithoutServiceReturns400() throws Exception {
        mockMvc.perform(post("/api/appointments").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startsAt\":\"2026-10-08T10:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El servicio es obligatorio"));
    }

    @Test
    void changesTheStatus() throws Exception {
        when(appointmentService.changeStatus(52L, AppointmentStatus.CANCELLED)).thenReturn(APPOINTMENT);

        mockMvc.perform(patch("/api/appointments/52/status").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());

        verify(appointmentService).changeStatus(52L, AppointmentStatus.CANCELLED);
    }

    @Test
    void linksAClient() throws Exception {
        when(appointmentService.linkClient(52L, 7L)).thenReturn(APPOINTMENT);

        mockMvc.perform(patch("/api/appointments/52/client").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":7}"))
                .andExpect(status().isOk());
    }

    @Test
    void returnsTheFreeTimesOfADay() throws Exception {
        when(appointmentService.availability(eq(10L), eq(LocalDate.of(2026, 10, 8)), eq(null)))
                .thenReturn(new DayAvailabilityResponse(LocalDate.of(2026, 10, 8), 30, List.of("09:00", "09:30")));

        mockMvc.perform(get("/api/appointments/availability").with(jwt())
                        .param("serviceId", "10").param("date", "2026-10-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(30))
                .andExpect(jsonPath("$.times[1]").value("09:30"));
    }
}
