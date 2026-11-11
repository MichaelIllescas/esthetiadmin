package com.esthetiadmin.backend.report.repository;

import com.esthetiadmin.backend.catalog.dto.CategoryRequest;
import com.esthetiadmin.backend.catalog.dto.OfferingRequest;
import com.esthetiadmin.backend.catalog.dto.OfferingResponse;
import com.esthetiadmin.backend.catalog.service.CategoryService;
import com.esthetiadmin.backend.catalog.service.OfferingService;
import com.esthetiadmin.backend.client.dto.ClientRequest;
import com.esthetiadmin.backend.client.dto.ClientResponse;
import com.esthetiadmin.backend.client.service.ClientService;
import com.esthetiadmin.backend.expense.dto.ExpenseRequest;
import com.esthetiadmin.backend.expense.dto.ExpenseTypeResponse;
import com.esthetiadmin.backend.expense.service.ExpenseService;
import com.esthetiadmin.backend.report.dto.IncomeBreakdownResponse;
import com.esthetiadmin.backend.report.dto.IncomeBreakdownResponse.CategoryIncome;
import com.esthetiadmin.backend.report.dto.MonthlyResponse;
import com.esthetiadmin.backend.report.dto.SummaryResponse;
import com.esthetiadmin.backend.report.dto.TopClientResponse;
import com.esthetiadmin.backend.report.dto.TopServiceResponse;
import com.esthetiadmin.backend.report.service.ReportService;
import com.esthetiadmin.backend.report.service.ReportService.ClientRanking;
import com.esthetiadmin.backend.treatment.dto.TreatmentRequest;
import com.esthetiadmin.backend.treatment.service.TreatmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the report queries against the real database, which is the only way to check the SQL itself. It uses
 * dates in 2001, where there is no real data, and a transaction that is rolled back, so nothing is left behind.
 */
@SpringBootTest
@Transactional
class ReportDatabaseTest {

    private static final LocalDate FROM = LocalDate.of(2001, 3, 1);
    private static final LocalDate TO = LocalDate.of(2001, 5, 31);

    @Autowired private ReportService reports;
    @Autowired private TreatmentService treatments;
    @Autowired private ExpenseService expenses;
    @Autowired private CategoryService categories;
    @Autowired private OfferingService offerings;
    @Autowired private ClientService clients;

    private OfferingResponse bozo;
    private OfferingResponse armpits;
    private OfferingResponse cleaning;
    private ClientResponse carina;
    private ClientResponse rosa;

    @BeforeEach
    void setUp() {
        Long lasers = categories.create(new CategoryRequest("Zzlaser", null)).id();
        Long facials = categories.create(new CategoryRequest("Zzfacial", null)).id();
        bozo = offerings.create(new OfferingRequest(lasers, "Zzbozo", null, new BigDecimal("1000"), null));
        armpits = offerings.create(new OfferingRequest(lasers, "Zzaxilas", null, new BigDecimal("2000"), null));
        cleaning = offerings.create(new OfferingRequest(facials, "Zzlimpieza", null, new BigDecimal("4000"), null));
        carina = clients.create(new ClientRequest("Zzcarina", "Zzdiaz", null, null, null, null));
        rosa = clients.create(new ClientRequest("Zzrosa", "Zzfunes", null, null, null, null));

        // March: 4.500 · April: 300 (three cheap ones) · May: 4.000 plus a free treatment
        treat(carina, bozo, "2001-03-10", "1000");
        treat(carina, armpits, "2001-03-20", "2000");
        treat(rosa, bozo, "2001-03-25", "1500");
        treat(rosa, bozo, "2001-04-01", "100");
        treat(rosa, bozo, "2001-04-02", "100");
        treat(rosa, bozo, "2001-04-03", "100");
        treat(carina, cleaning, "2001-05-05", "4000");
        treat(rosa, bozo, "2001-05-06", "0");
        // Outside the period: must not count anywhere
        treat(carina, bozo, "2001-02-28", "9999");
        treat(carina, bozo, "2001-06-01", "9999");

        ExpenseTypeResponse type = expenses.listTypes().get(0);
        spend(type, "2001-03-15", "800");
        spend(type, "2001-04-15", "300");
        spend(type, "2001-05-15", "5000");
        spend(type, "2001-07-01", "999");
    }

    private void treat(ClientResponse client, OfferingResponse offering, String day, String amount) {
        treatments.create(new TreatmentRequest(client.id(), offering.id(), LocalDate.parse(day), new BigDecimal(amount), null));
    }

    private void spend(ExpenseTypeResponse type, String day, String amount) {
        expenses.create(new ExpenseRequest(LocalDate.parse(day), "Zzgasto", new BigDecimal(amount), type.id()));
    }

    @Test
    void summaryAddsUpOnlyThePeriodAndCountsFreeTreatments() {
        SummaryResponse summary = reports.summary(FROM, TO);

        assertThat(summary.income()).isEqualByComparingTo("8800");
        assertThat(summary.expenses()).isEqualByComparingTo("6100");
        assertThat(summary.balance()).isEqualByComparingTo("2700");
        assertThat(summary.treatmentsCount()).isEqualTo(8);
        assertThat(summary.expensesCount()).isEqualTo(3);
    }

    @Test
    void aPeriodWithoutMovementIsAllZeros() {
        SummaryResponse summary = reports.summary(LocalDate.of(2001, 10, 1), LocalDate.of(2001, 10, 31));

        assertThat(summary.income()).isEqualByComparingTo("0");
        assertThat(summary.expenses()).isEqualByComparingTo("0");
        assertThat(summary.balance()).isEqualByComparingTo("0");
        assertThat(summary.treatmentsCount()).isZero();
    }

    @Test
    void thePeriodIncludesItsFirstAndItsLastDay() {
        SummaryResponse onlyTheEdges = reports.summary(LocalDate.of(2001, 3, 10), LocalDate.of(2001, 3, 10));
        assertThat(onlyTheEdges.income()).isEqualByComparingTo("1000");

        SummaryResponse lastDay = reports.summary(LocalDate.of(2001, 5, 6), LocalDate.of(2001, 5, 6));
        assertThat(lastDay.treatmentsCount()).isEqualTo(1);
        assertThat(lastDay.income()).isEqualByComparingTo("0");
    }

    @Test
    void monthlyReturnsEveryMonthIncludingTheOneWithoutIncome() {
        List<MonthlyResponse> months = reports.monthly(FROM, TO);

        assertThat(months).extracting(MonthlyResponse::month).containsExactly("2001-03", "2001-04", "2001-05");
        assertThat(months).extracting(m -> m.income().intValue()).containsExactly(4500, 300, 4000);
        assertThat(months).extracting(m -> m.expenses().intValue()).containsExactly(800, 300, 5000);
        assertThat(months).extracting(m -> m.balance().intValue()).containsExactly(3700, 0, -1000);
    }

    @Test
    void monthlyFillsAMonthWithNoMovementAtAll() {
        List<MonthlyResponse> months = reports.monthly(LocalDate.of(2001, 5, 1), LocalDate.of(2001, 7, 31));

        assertThat(months).extracting(MonthlyResponse::month).containsExactly("2001-05", "2001-06", "2001-07");
        assertThat(months.get(1).income()).isEqualByComparingTo("9999");
        assertThat(months.get(2).expenses()).isEqualByComparingTo("999");
    }

    @Test
    void topServicesRanksByHowManyTimesTheyWerePerformed() {
        List<TopServiceResponse> top = reports.topServices(FROM, TO, 10);

        assertThat(top).extracting(TopServiceResponse::serviceName).containsExactly("Zzbozo", "Zzlimpieza", "Zzaxilas");
        assertThat(top.get(0).treatments()).isEqualTo(6);
        assertThat(top.get(0).income()).isEqualByComparingTo("2800");
        assertThat(top.get(0).categoryName()).isEqualTo("Zzlaser");
        assertThat(reports.topServices(FROM, TO, 2)).extracting(TopServiceResponse::serviceName)
                .containsExactly("Zzbozo", "Zzlimpieza");
    }

    @Test
    void incomeByServiceGroupsByCategoryWithPercentages() {
        IncomeBreakdownResponse breakdown = reports.incomeByService(FROM, TO);

        assertThat(breakdown.totalIncome()).isEqualByComparingTo("8800");
        assertThat(breakdown.categories()).extracting(CategoryIncome::categoryName).containsExactly("Zzlaser", "Zzfacial");
        CategoryIncome laser = breakdown.categories().get(0);
        assertThat(laser.income()).isEqualByComparingTo("4800");
        assertThat(laser.percentage()).isEqualByComparingTo("54.55");
        assertThat(laser.treatments()).isEqualTo(7);
        assertThat(laser.services()).extracting(s -> s.serviceName()).containsExactly("Zzbozo", "Zzaxilas");
        assertThat(laser.services().get(0).percentage()).isEqualByComparingTo("31.82");
        assertThat(breakdown.categories().get(1).percentage()).isEqualByComparingTo("45.45");
    }

    @Test
    void incomeByServiceStillCountsDeactivatedServicesAndCategories() {
        offerings.setActive(armpits.id(), false);
        categories.setActive(categories.findAll().stream().filter(c -> c.name().equals("Zzfacial")).findFirst().orElseThrow().id(), false);

        IncomeBreakdownResponse breakdown = reports.incomeByService(FROM, TO);

        assertThat(breakdown.totalIncome()).isEqualByComparingTo("8800");
        assertThat(breakdown.categories()).extracting(CategoryIncome::categoryName).containsExactly("Zzlaser", "Zzfacial");
    }

    @Test
    void topClientsCanBeOrderedByAmountOrByNumberOfTreatments() {
        List<TopClientResponse> byAmount = reports.topClients(FROM, TO, 10, ClientRanking.AMOUNT);
        assertThat(byAmount).extracting(TopClientResponse::clientName).containsExactly("Zzdiaz, Zzcarina", "Zzfunes, Zzrosa");
        assertThat(byAmount.get(0).totalAmount()).isEqualByComparingTo("7000");
        assertThat(byAmount.get(0).treatments()).isEqualTo(3);

        List<TopClientResponse> byTreatments = reports.topClients(FROM, TO, 10, ClientRanking.TREATMENTS);
        assertThat(byTreatments).extracting(TopClientResponse::clientName).containsExactly("Zzfunes, Zzrosa", "Zzdiaz, Zzcarina");
        assertThat(byTreatments.get(0).treatments()).isEqualTo(5);
        assertThat(byTreatments.get(0).totalAmount()).isEqualByComparingTo("1800");
    }

    @Test
    void topClientsAppliesTheLimit() {
        assertThat(reports.topClients(FROM, TO, 1, ClientRanking.AMOUNT))
                .extracting(TopClientResponse::clientName).containsExactly("Zzdiaz, Zzcarina");
        assertThat(reports.topClients(FROM, TO, 1, ClientRanking.TREATMENTS))
                .extracting(TopClientResponse::clientName).containsExactly("Zzfunes, Zzrosa");
    }
}
