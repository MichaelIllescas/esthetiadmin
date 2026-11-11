package com.esthetiadmin.backend.report.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only aggregate queries for the reports. They join tables of several modules on purpose: a report is a
 * read model, and grouping in SQL is far cheaper than loading every treatment into memory. Both dates of every
 * range are inclusive.
 */
@Repository
@RequiredArgsConstructor
public class ReportQueries {

    private final JdbcClient jdbc;

    public record MoneyTotal(BigDecimal total, long entries) {
    }

    public record MonthTotal(String month, BigDecimal total) {
    }

    public record ServiceRow(Long serviceId, String serviceName, Long categoryId, String categoryName,
                             long treatments, BigDecimal income) {
    }

    public record ClientRow(Long clientId, String lastName, String firstName, long treatments, BigDecimal total) {
    }

    public MoneyTotal treatmentTotals(LocalDate from, LocalDate to) {
        return jdbc.sql("""
                        SELECT COALESCE(SUM(amount), 0) AS total, COUNT(*) AS entries
                        FROM treatment
                        WHERE performed_on BETWEEN :from AND :to
                        """)
                .param("from", from).param("to", to)
                .query((rs, row) -> new MoneyTotal(rs.getBigDecimal("total"), rs.getLong("entries")))
                .single();
    }

    public MoneyTotal expenseTotals(LocalDate from, LocalDate to) {
        return jdbc.sql("""
                        SELECT COALESCE(SUM(amount), 0) AS total, COUNT(*) AS entries
                        FROM expense
                        WHERE spent_on BETWEEN :from AND :to
                        """)
                .param("from", from).param("to", to)
                .query((rs, row) -> new MoneyTotal(rs.getBigDecimal("total"), rs.getLong("entries")))
                .single();
    }

    /** Income per month ("2026-09"). Months without treatments are not returned. */
    public List<MonthTotal> incomeByMonth(LocalDate from, LocalDate to) {
        return jdbc.sql("""
                        SELECT DATE_FORMAT(performed_on, '%Y-%m') AS period, SUM(amount) AS total
                        FROM treatment
                        WHERE performed_on BETWEEN :from AND :to
                        GROUP BY DATE_FORMAT(performed_on, '%Y-%m')
                        """)
                .param("from", from).param("to", to)
                .query((rs, row) -> new MonthTotal(rs.getString("period"), rs.getBigDecimal("total")))
                .list();
    }

    /** Expenses per month ("2026-09"). Months without expenses are not returned. */
    public List<MonthTotal> expensesByMonth(LocalDate from, LocalDate to) {
        return jdbc.sql("""
                        SELECT DATE_FORMAT(spent_on, '%Y-%m') AS period, SUM(amount) AS total
                        FROM expense
                        WHERE spent_on BETWEEN :from AND :to
                        GROUP BY DATE_FORMAT(spent_on, '%Y-%m')
                        """)
                .param("from", from).param("to", to)
                .query((rs, row) -> new MonthTotal(rs.getString("period"), rs.getBigDecimal("total")))
                .list();
    }

    /**
     * Treatments and income per service, with its category. Includes deactivated services and categories,
     * because what was done stays in the history. Most performed first.
     */
    public List<ServiceRow> servicesInPeriod(LocalDate from, LocalDate to) {
        return jdbc.sql("""
                        SELECT s.id AS service_id, s.name AS service_name, c.id AS category_id, c.name AS category_name,
                               COUNT(*) AS treatments, SUM(t.amount) AS income
                        FROM treatment t
                        JOIN service s ON s.id = t.service_id
                        JOIN category c ON c.id = s.category_id
                        WHERE t.performed_on BETWEEN :from AND :to
                        GROUP BY s.id, s.name, c.id, c.name
                        ORDER BY treatments DESC, income DESC, s.name ASC
                        """)
                .param("from", from).param("to", to)
                .query((rs, row) -> new ServiceRow(rs.getLong("service_id"), rs.getString("service_name"),
                        rs.getLong("category_id"), rs.getString("category_name"),
                        rs.getLong("treatments"), rs.getBigDecimal("income")))
                .list();
    }

    /** Treatments and total charged per client. The caller chooses the order, which is limited to a fixed list. */
    public List<ClientRow> clientsInPeriod(LocalDate from, LocalDate to, boolean byTreatments, int limit) {
        String order = byTreatments
                ? "treatments DESC, total DESC, cl.last_name ASC, cl.first_name ASC"
                : "total DESC, treatments DESC, cl.last_name ASC, cl.first_name ASC";
        return jdbc.sql("""
                        SELECT cl.id AS client_id, cl.last_name AS last_name, cl.first_name AS first_name,
                               COUNT(*) AS treatments, SUM(t.amount) AS total
                        FROM treatment t
                        JOIN client cl ON cl.id = t.client_id
                        WHERE t.performed_on BETWEEN :from AND :to
                        GROUP BY cl.id, cl.last_name, cl.first_name
                        ORDER BY\s""" + order + """
                         LIMIT :limit
                        """)
                .param("from", from).param("to", to).param("limit", limit)
                .query((rs, row) -> new ClientRow(rs.getLong("client_id"), rs.getString("last_name"),
                        rs.getString("first_name"), rs.getLong("treatments"), rs.getBigDecimal("total")))
                .list();
    }
}
