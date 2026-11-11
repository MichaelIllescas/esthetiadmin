package com.esthetiadmin.backend.report.service;

import com.esthetiadmin.backend.common.exception.BadRequestException;
import com.esthetiadmin.backend.report.dto.IncomeBreakdownResponse;
import com.esthetiadmin.backend.report.dto.MonthlyResponse;
import com.esthetiadmin.backend.report.dto.SummaryResponse;
import com.esthetiadmin.backend.report.dto.TopClientResponse;
import com.esthetiadmin.backend.report.dto.TopServiceResponse;
import com.esthetiadmin.backend.report.repository.ReportQueries;
import com.esthetiadmin.backend.report.repository.ReportQueries.ClientRow;
import com.esthetiadmin.backend.report.repository.ReportQueries.MoneyTotal;
import com.esthetiadmin.backend.report.repository.ReportQueries.MonthTotal;
import com.esthetiadmin.backend.report.repository.ReportQueries.ServiceRow;
import com.esthetiadmin.backend.report.service.ReportService.ClientRanking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);

    private ReportQueries queries;
    private ReportService service;

    @BeforeEach
    void setUp() {
        queries = mock(ReportQueries.class);
        service = new ReportService(queries);
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static ServiceRow row(long serviceId, String service, long categoryId, String category, long treatments, String income) {
        return new ServiceRow(serviceId, service, categoryId, category, treatments, money(income));
    }

    @Test
    void summaryComputesTheBalanceAndTheCounts() {
        when(queries.treatmentTotals(FROM, TO)).thenReturn(new MoneyTotal(money("50000.00"), 4));
        when(queries.expenseTotals(FROM, TO)).thenReturn(new MoneyTotal(money("32000.50"), 2));

        SummaryResponse summary = service.summary(FROM, TO);

        assertThat(summary.income()).isEqualByComparingTo("50000.00");
        assertThat(summary.expenses()).isEqualByComparingTo("32000.50");
        assertThat(summary.balance()).isEqualByComparingTo("17999.50");
        assertThat(summary.treatmentsCount()).isEqualTo(4);
        assertThat(summary.expensesCount()).isEqualTo(2);
        assertThat(summary.from()).isEqualTo(FROM);
        assertThat(summary.to()).isEqualTo(TO);
    }

    @Test
    void summaryCanBeNegativeAndCanBeEmpty() {
        when(queries.treatmentTotals(FROM, TO)).thenReturn(new MoneyTotal(money("1000"), 1));
        when(queries.expenseTotals(FROM, TO)).thenReturn(new MoneyTotal(money("4000"), 1));
        assertThat(service.summary(FROM, TO).balance()).isEqualByComparingTo("-3000");

        when(queries.treatmentTotals(FROM, TO)).thenReturn(new MoneyTotal(BigDecimal.ZERO, 0));
        when(queries.expenseTotals(FROM, TO)).thenReturn(new MoneyTotal(BigDecimal.ZERO, 0));
        assertThat(service.summary(FROM, TO).balance()).isEqualByComparingTo("0");
    }

    @Test
    void everyReportRejectsAMissingPeriod() {
        assertThatThrownBy(() -> service.summary(null, TO)).isInstanceOf(BadRequestException.class).hasMessageContaining("Indicá el período");
        assertThatThrownBy(() -> service.monthly(FROM, null)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.topServices(null, null, 10)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.incomeByService(null, TO)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.topClients(FROM, null, 10, ClientRanking.AMOUNT)).isInstanceOf(BadRequestException.class);
        verify(queries, never()).treatmentTotals(any(), any());
    }

    @Test
    void everyReportRejectsAnInvertedPeriod() {
        assertThatThrownBy(() -> service.summary(TO, FROM))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no puede ser posterior");
        assertThatThrownBy(() -> service.monthly(TO, FROM)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void thePeriodCannotExceedFiveYearsButFiveIsFine() {
        when(queries.treatmentTotals(any(), any())).thenReturn(new MoneyTotal(BigDecimal.ZERO, 0));
        when(queries.expenseTotals(any(), any())).thenReturn(new MoneyTotal(BigDecimal.ZERO, 0));

        assertThatCode(() -> service.summary(LocalDate.of(2021, 9, 26), LocalDate.of(2026, 9, 26))).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.summary(LocalDate.of(2021, 9, 26), LocalDate.of(2026, 9, 27)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("5 años");
    }

    @Test
    void monthlyFillsTheMonthsWithoutMovementWithZeros() {
        LocalDate from = LocalDate.of(2026, 1, 15);
        LocalDate to = LocalDate.of(2026, 4, 10);
        when(queries.incomeByMonth(from, to)).thenReturn(List.of(
                new MonthTotal("2026-01", money("1000")), new MonthTotal("2026-04", money("400"))));
        when(queries.expensesByMonth(from, to)).thenReturn(List.of(
                new MonthTotal("2026-01", money("300")), new MonthTotal("2026-03", money("900"))));

        List<MonthlyResponse> months = service.monthly(from, to);

        assertThat(months).extracting(MonthlyResponse::month).containsExactly("2026-01", "2026-02", "2026-03", "2026-04");
        assertThat(months).extracting(m -> m.income().intValue()).containsExactly(1000, 0, 0, 400);
        assertThat(months).extracting(m -> m.expenses().intValue()).containsExactly(300, 0, 900, 0);
        assertThat(months).extracting(m -> m.balance().intValue()).containsExactly(700, 0, -900, 400);
    }

    @Test
    void monthlyCrossesTheEndOfTheYearAndHandlesASingleMonth() {
        LocalDate from = LocalDate.of(2025, 11, 20);
        LocalDate to = LocalDate.of(2026, 2, 3);
        when(queries.incomeByMonth(from, to)).thenReturn(List.of());
        when(queries.expensesByMonth(from, to)).thenReturn(List.of());
        assertThat(service.monthly(from, to)).extracting(MonthlyResponse::month)
                .containsExactly("2025-11", "2025-12", "2026-01", "2026-02");

        LocalDate day = LocalDate.of(2026, 9, 10);
        when(queries.incomeByMonth(day, day)).thenReturn(List.of());
        when(queries.expensesByMonth(day, day)).thenReturn(List.of());
        assertThat(service.monthly(day, day)).extracting(MonthlyResponse::month).containsExactly("2026-09");
    }

    @Test
    void topServicesKeepsTheOrderOfTheQueryAndAppliesTheLimit() {
        when(queries.servicesInPeriod(FROM, TO)).thenReturn(List.of(
                row(10, "Bozo", 1, "Depilación", 6, "2800"),
                row(20, "Limpieza", 2, "Faciales", 1, "4000"),
                row(11, "Axilas", 1, "Depilación", 1, "2000")));

        List<TopServiceResponse> top = service.topServices(FROM, TO, 2);

        assertThat(top).extracting(TopServiceResponse::serviceName).containsExactly("Bozo", "Limpieza");
        assertThat(top.get(0).treatments()).isEqualTo(6);
        assertThat(top.get(0).categoryName()).isEqualTo("Depilación");
        assertThat(top.get(0).income()).isEqualByComparingTo("2800");
    }

    @Test
    void incomeByServiceGroupsByCategoryWithPercentagesOfTheTotal() {
        when(queries.servicesInPeriod(FROM, TO)).thenReturn(List.of(
                row(10, "Bozo", 1, "Depilación", 6, "2800"),
                row(20, "Limpieza", 2, "Faciales", 1, "4000"),
                row(11, "Axilas", 1, "Depilación", 1, "2000")));

        IncomeBreakdownResponse breakdown = service.incomeByService(FROM, TO);

        assertThat(breakdown.totalIncome()).isEqualByComparingTo("8800");
        assertThat(breakdown.categories()).extracting(IncomeBreakdownResponse.CategoryIncome::categoryName)
                .containsExactly("Depilación", "Faciales");
        IncomeBreakdownResponse.CategoryIncome laser = breakdown.categories().get(0);
        assertThat(laser.income()).isEqualByComparingTo("4800");
        assertThat(laser.percentage()).isEqualByComparingTo("54.55");
        assertThat(laser.treatments()).isEqualTo(7);
        assertThat(laser.services()).extracting(IncomeBreakdownResponse.ServiceIncome::serviceName)
                .containsExactly("Bozo", "Axilas");
        assertThat(laser.services().get(0).percentage()).isEqualByComparingTo("31.82");
        assertThat(laser.services().get(1).percentage()).isEqualByComparingTo("22.73");
        assertThat(breakdown.categories().get(1).percentage()).isEqualByComparingTo("45.45");
    }

    @Test
    void incomeByServiceOrdersCategoriesByIncomeNotByTreatments() {
        when(queries.servicesInPeriod(FROM, TO)).thenReturn(List.of(
                row(10, "Bozo", 1, "Depilación", 9, "900"),
                row(20, "Exosomas", 2, "Faciales", 1, "35000")));

        assertThat(service.incomeByService(FROM, TO).categories())
                .extracting(IncomeBreakdownResponse.CategoryIncome::categoryName)
                .containsExactly("Faciales", "Depilación");
    }

    @Test
    void incomeByServiceWithNothingInThePeriodReturnsZeroWithoutDividingByZero() {
        when(queries.servicesInPeriod(FROM, TO)).thenReturn(List.of());

        IncomeBreakdownResponse breakdown = service.incomeByService(FROM, TO);

        assertThat(breakdown.totalIncome()).isEqualByComparingTo("0");
        assertThat(breakdown.categories()).isEmpty();
    }

    @Test
    void freeTreatmentsCountButDoNotAddIncome() {
        when(queries.servicesInPeriod(FROM, TO)).thenReturn(List.of(row(10, "Bozo", 1, "Depilación", 3, "0.00")));

        IncomeBreakdownResponse breakdown = service.incomeByService(FROM, TO);

        assertThat(breakdown.categories().get(0).treatments()).isEqualTo(3);
        assertThat(breakdown.categories().get(0).percentage()).isEqualByComparingTo("0");
    }

    @Test
    void topClientsShowsTheNameAsLastNameFirstNameAndPassesTheOrderAndTheLimit() {
        when(queries.clientsInPeriod(FROM, TO, true, 5)).thenReturn(List.of(
                new ClientRow(2L, "Funes", "Rosa", 5, money("1800"))));

        List<TopClientResponse> top = service.topClients(FROM, TO, 5, ClientRanking.TREATMENTS);

        assertThat(top).hasSize(1);
        assertThat(top.get(0).clientName()).isEqualTo("Funes, Rosa");
        assertThat(top.get(0).treatments()).isEqualTo(5);
        assertThat(top.get(0).totalAmount()).isEqualByComparingTo("1800");
        verify(queries).clientsInPeriod(FROM, TO, true, 5);
    }

    @Test
    void topClientsByAmountIsTheDefaultOrder() {
        when(queries.clientsInPeriod(any(), any(), anyBoolean(), anyInt())).thenReturn(List.of());

        service.topClients(FROM, TO, 10, ClientRanking.AMOUNT);

        verify(queries).clientsInPeriod(FROM, TO, false, 10);
    }

    @Test
    void theRankingOrderIsParsedFromTheParameter() {
        assertThat(ClientRanking.from(null)).isEqualTo(ClientRanking.AMOUNT);
        assertThat(ClientRanking.from("")).isEqualTo(ClientRanking.AMOUNT);
        assertThat(ClientRanking.from("amount")).isEqualTo(ClientRanking.AMOUNT);
        assertThat(ClientRanking.from("TREATMENTS")).isEqualTo(ClientRanking.TREATMENTS);
        assertThat(ClientRanking.from("treatments")).isEqualTo(ClientRanking.TREATMENTS);
        assertThatThrownBy(() -> ClientRanking.from("nombre"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("amount");
    }
}
