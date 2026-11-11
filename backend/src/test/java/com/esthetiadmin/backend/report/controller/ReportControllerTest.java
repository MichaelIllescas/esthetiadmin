package com.esthetiadmin.backend.report.controller;

import com.esthetiadmin.backend.auth.security.ApiSecurityErrorHandler;
import com.esthetiadmin.backend.auth.security.JwtConfig;
import com.esthetiadmin.backend.auth.security.SecurityConfig;
import com.esthetiadmin.backend.common.config.CorsConfig;
import com.esthetiadmin.backend.common.exception.BadRequestException;
import com.esthetiadmin.backend.common.exception.GlobalExceptionHandler;
import com.esthetiadmin.backend.report.dto.IncomeBreakdownResponse;
import com.esthetiadmin.backend.report.dto.IncomeBreakdownResponse.CategoryIncome;
import com.esthetiadmin.backend.report.dto.IncomeBreakdownResponse.ServiceIncome;
import com.esthetiadmin.backend.report.dto.MonthlyResponse;
import com.esthetiadmin.backend.report.dto.SummaryResponse;
import com.esthetiadmin.backend.report.dto.TopClientResponse;
import com.esthetiadmin.backend.report.dto.TopServiceResponse;
import com.esthetiadmin.backend.report.service.ReportService;
import com.esthetiadmin.backend.report.service.ReportService.ClientRanking;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, JwtConfig.class, CorsConfig.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-with-more-than-thirty-two-characters",
        "app.security.jwt.expiration-minutes=60",
        "app.cors.allowed-origins=http://localhost:4200"
})
class ReportControllerTest {

    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @Test
    void everyReportRequiresAuthentication() throws Exception {
        for (String path : List.of("summary", "monthly", "top-services", "income-by-service", "top-clients")) {
            mockMvc.perform(get("/api/reports/" + path).param("from", "2026-09-01").param("to", "2026-09-30"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void summaryReturnsTheTotalsOfThePeriod() throws Exception {
        when(reportService.summary(FROM, TO)).thenReturn(new SummaryResponse(FROM, TO, new BigDecimal("50000.00"),
                new BigDecimal("32000.00"), new BigDecimal("18000.00"), 4, 2));

        mockMvc.perform(get("/api/reports/summary").with(jwt()).param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.income").value(50000.00))
                .andExpect(jsonPath("$.expenses").value(32000.00))
                .andExpect(jsonPath("$.balance").value(18000.00))
                .andExpect(jsonPath("$.treatmentsCount").value(4))
                .andExpect(jsonPath("$.from").value("2026-09-01"));
    }

    @Test
    void aMissingPeriodIsAnsweredWithTheApiMessage() throws Exception {
        when(reportService.summary(null, null)).thenThrow(new BadRequestException("Indicá el período: la fecha \"desde\" y la fecha \"hasta\""));

        mockMvc.perform(get("/api/reports/summary").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Indicá el período: la fecha \"desde\" y la fecha \"hasta\""));
    }

    @Test
    void aMalformedDateReturns400() throws Exception {
        mockMvc.perform(get("/api/reports/summary").with(jwt()).param("from", "01/09/2026").param("to", "2026-09-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El valor del parámetro 'from' no es válido"));
    }

    @Test
    void monthlyReturnsEveryMonthOfThePeriod() throws Exception {
        when(reportService.monthly(FROM, TO)).thenReturn(List.of(
                new MonthlyResponse("2026-09", new BigDecimal("1000"), new BigDecimal("400"), new BigDecimal("600"))));

        mockMvc.perform(get("/api/reports/monthly").with(jwt()).param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value("2026-09"))
                .andExpect(jsonPath("$[0].balance").value(600));
    }

    @Test
    void topServicesUsesTenAsTheDefaultLimit() throws Exception {
        when(reportService.topServices(FROM, TO, 10)).thenReturn(List.of(
                new TopServiceResponse(10L, "Bozo", 1L, "Depilación", 6, new BigDecimal("2800"))));

        mockMvc.perform(get("/api/reports/top-services").with(jwt()).param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("Bozo"))
                .andExpect(jsonPath("$[0].treatments").value(6));
    }

    @Test
    void topServicesRejectsAnInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/reports/top-services").with(jwt())
                        .param("from", "2026-09-01").param("to", "2026-09-30").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("El límite debe ser al menos 1"));
        mockMvc.perform(get("/api/reports/top-services").with(jwt())
                        .param("from", "2026-09-01").param("to", "2026-09-30").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El límite no puede superar 100"));
    }

    @Test
    void incomeByServiceReturnsCategoriesWithTheirServices() throws Exception {
        ServiceIncome bozo = new ServiceIncome(10L, "Bozo", new BigDecimal("2800"), new BigDecimal("31.82"), 6);
        CategoryIncome laser = new CategoryIncome(1L, "Depilación", new BigDecimal("2800"), new BigDecimal("31.82"), 6, List.of(bozo));
        when(reportService.incomeByService(FROM, TO)).thenReturn(new IncomeBreakdownResponse(new BigDecimal("8800"), List.of(laser)));

        mockMvc.perform(get("/api/reports/income-by-service").with(jwt()).param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(8800))
                .andExpect(jsonPath("$.categories[0].categoryName").value("Depilación"))
                .andExpect(jsonPath("$.categories[0].percentage").value(31.82))
                .andExpect(jsonPath("$.categories[0].services[0].serviceName").value("Bozo"));
    }

    @Test
    void topClientsOrdersByAmountByDefault() throws Exception {
        when(reportService.topClients(FROM, TO, 10, ClientRanking.AMOUNT)).thenReturn(List.of(
                new TopClientResponse(1L, "Diaz, Carina", 3, new BigDecimal("7000"))));

        mockMvc.perform(get("/api/reports/top-clients").with(jwt()).param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clientName").value("Diaz, Carina"))
                .andExpect(jsonPath("$[0].totalAmount").value(7000));
    }

    @Test
    void topClientsCanBeOrderedByTreatments() throws Exception {
        when(reportService.topClients(any(), any(), eq(5), eq(ClientRanking.TREATMENTS))).thenReturn(List.of());

        mockMvc.perform(get("/api/reports/top-clients").with(jwt())
                        .param("from", "2026-09-01").param("to", "2026-09-30").param("limit", "5").param("sortBy", "treatments"))
                .andExpect(status().isOk());

        verify(reportService).topClients(FROM, TO, 5, ClientRanking.TREATMENTS);
    }

    @Test
    void topClientsRejectsAnUnknownOrder() throws Exception {
        mockMvc.perform(get("/api/reports/top-clients").with(jwt())
                        .param("from", "2026-09-01").param("to", "2026-09-30").param("sortBy", "nombre"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
