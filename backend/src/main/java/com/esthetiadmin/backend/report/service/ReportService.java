package com.esthetiadmin.backend.report.service;

import com.esthetiadmin.backend.common.exception.BadRequestException;
import com.esthetiadmin.backend.report.dto.IncomeBreakdownResponse;
import com.esthetiadmin.backend.report.dto.IncomeBreakdownResponse.CategoryIncome;
import com.esthetiadmin.backend.report.dto.IncomeBreakdownResponse.ServiceIncome;
import com.esthetiadmin.backend.report.dto.MonthlyResponse;
import com.esthetiadmin.backend.report.dto.SummaryResponse;
import com.esthetiadmin.backend.report.dto.TopClientResponse;
import com.esthetiadmin.backend.report.dto.TopServiceResponse;
import com.esthetiadmin.backend.report.repository.ReportQueries;
import com.esthetiadmin.backend.report.repository.ReportQueries.MoneyTotal;
import com.esthetiadmin.backend.report.repository.ReportQueries.MonthTotal;
import com.esthetiadmin.backend.report.repository.ReportQueries.ServiceRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    /** A wider range would mean thousands of months or rows for no practical use. */
    static final int MAX_YEARS = 5;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ReportQueries queries;

    public SummaryResponse summary(LocalDate from, LocalDate to) {
        validate(from, to);
        MoneyTotal income = queries.treatmentTotals(from, to);
        MoneyTotal expenses = queries.expenseTotals(from, to);
        return new SummaryResponse(from, to, income.total(), expenses.total(),
                income.total().subtract(expenses.total()), income.entries(), expenses.entries());
    }

    public List<MonthlyResponse> monthly(LocalDate from, LocalDate to) {
        validate(from, to);
        Map<String, BigDecimal> income = toMap(queries.incomeByMonth(from, to));
        Map<String, BigDecimal> expenses = toMap(queries.expensesByMonth(from, to));

        List<MonthlyResponse> months = new ArrayList<>();
        for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to)); month = month.plusMonths(1)) {
            BigDecimal monthIncome = income.getOrDefault(month.toString(), BigDecimal.ZERO);
            BigDecimal monthExpenses = expenses.getOrDefault(month.toString(), BigDecimal.ZERO);
            months.add(new MonthlyResponse(month.toString(), monthIncome, monthExpenses,
                    monthIncome.subtract(monthExpenses)));
        }
        return months;
    }

    /** The most performed services first; ties are broken by income and then by name. */
    public List<TopServiceResponse> topServices(LocalDate from, LocalDate to, int limit) {
        validate(from, to);
        return queries.servicesInPeriod(from, to).stream()
                .limit(limit)
                .map(row -> new TopServiceResponse(row.serviceId(), row.serviceName(), row.categoryId(),
                        row.categoryName(), row.treatments(), row.income()))
                .toList();
    }

    public IncomeBreakdownResponse incomeByService(LocalDate from, LocalDate to) {
        validate(from, to);
        List<ServiceRow> rows = queries.servicesInPeriod(from, to);
        BigDecimal total = rows.stream().map(ServiceRow::income).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, List<ServiceRow>> byCategory = rows.stream()
                .collect(Collectors.groupingBy(ServiceRow::categoryId, LinkedHashMap::new, Collectors.toList()));

        List<CategoryIncome> categories = byCategory.values().stream()
                .map(services -> toCategory(services, total))
                .sorted((a, b) -> b.income().compareTo(a.income()))
                .toList();
        return new IncomeBreakdownResponse(total, categories);
    }

    public List<TopClientResponse> topClients(LocalDate from, LocalDate to, int limit, ClientRanking ranking) {
        validate(from, to);
        return queries.clientsInPeriod(from, to, ranking == ClientRanking.TREATMENTS, limit).stream()
                .map(row -> new TopClientResponse(row.clientId(), row.lastName() + ", " + row.firstName(),
                        row.treatments(), row.total()))
                .toList();
    }

    /** How the client ranking is ordered: by what they paid or by how many treatments they had. */
    public enum ClientRanking {
        AMOUNT, TREATMENTS;

        public static ClientRanking from(String value) {
            if (value == null || value.isBlank() || value.equalsIgnoreCase("amount")) {
                return AMOUNT;
            }
            if (value.equalsIgnoreCase("treatments")) {
                return TREATMENTS;
            }
            throw new BadRequestException("El orden del ranking debe ser \"amount\" o \"treatments\"");
        }
    }

    private CategoryIncome toCategory(List<ServiceRow> services, BigDecimal grandTotal) {
        ServiceRow first = services.get(0);
        BigDecimal income = services.stream().map(ServiceRow::income).reduce(BigDecimal.ZERO, BigDecimal::add);
        long treatments = services.stream().mapToLong(ServiceRow::treatments).sum();
        List<ServiceIncome> details = services.stream()
                .sorted((a, b) -> b.income().compareTo(a.income()))
                .map(row -> new ServiceIncome(row.serviceId(), row.serviceName(), row.income(),
                        percentage(row.income(), grandTotal), row.treatments()))
                .toList();
        return new CategoryIncome(first.categoryId(), first.categoryName(), income,
                percentage(income, grandTotal), treatments, details);
    }

    private BigDecimal percentage(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return part.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> toMap(List<MonthTotal> totals) {
        return totals.stream().collect(Collectors.toMap(MonthTotal::month, MonthTotal::total, BigDecimal::add));
    }

    private void validate(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("Indicá el período: la fecha \"desde\" y la fecha \"hasta\"");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("La fecha \"desde\" no puede ser posterior a la fecha \"hasta\"");
        }
        if (to.isAfter(from.plusYears(MAX_YEARS))) {
            throw new BadRequestException("El período no puede superar los " + MAX_YEARS + " años");
        }
    }
}
