package com.esthetiadmin.backend.report.controller;

import com.esthetiadmin.backend.report.dto.IncomeBreakdownResponse;
import com.esthetiadmin.backend.report.dto.MonthlyResponse;
import com.esthetiadmin.backend.report.dto.SummaryResponse;
import com.esthetiadmin.backend.report.dto.TopClientResponse;
import com.esthetiadmin.backend.report.dto.TopServiceResponse;
import com.esthetiadmin.backend.report.service.ReportService;
import com.esthetiadmin.backend.report.service.ReportService.ClientRanking;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Financial reports. The period is always {@code from} and {@code to}, both included; they are declared optional
 * here only so a missing one is answered with the API's own message instead of a generic one.
 */
@Validated
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController implements ReportControllerDocs {

    private final ReportService reportService;

    @Override
    @GetMapping("/summary")
    public SummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.summary(from, to);
    }

    @Override
    @GetMapping("/monthly")
    public List<MonthlyResponse> monthly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.monthly(from, to);
    }

    @Override
    @GetMapping("/top-services")
    public List<TopServiceResponse> topServices(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        return reportService.topServices(from, to, limit);
    }

    @Override
    @GetMapping("/income-by-service")
    public IncomeBreakdownResponse incomeByService(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.incomeByService(from, to);
    }

    @Override
    @GetMapping("/top-clients")
    public List<TopClientResponse> topClients(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String sortBy) {
        return reportService.topClients(from, to, limit, ClientRanking.from(sortBy));
    }
}
