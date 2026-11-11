package com.esthetiadmin.backend.availability.repository;

import com.esthetiadmin.backend.availability.dto.AvailabilityBlockRequest;
import com.esthetiadmin.backend.availability.dto.AvailabilityRuleRequest;
import com.esthetiadmin.backend.availability.dto.AvailabilityRuleResponse;
import com.esthetiadmin.backend.availability.service.AvailabilityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against the real database: checks the V4 tables, their constraints and that replacing the week really
 * removes the previous ranges. Everything is rolled back.
 */
@SpringBootTest
@Transactional
class AvailabilityDatabaseTest {

    @Autowired
    private AvailabilityService service;

    @Autowired
    private AvailabilityRuleRepository ruleRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void replacingTheWeekLeavesOnlyTheNewRanges() {
        service.replaceRules(List.of(new AvailabilityRuleRequest(1, LocalTime.of(9, 0), LocalTime.of(13, 0))));
        List<AvailabilityRuleResponse> week = service.replaceRules(List.of(
                new AvailabilityRuleRequest(5, LocalTime.of(15, 0), LocalTime.of(19, 0)),
                new AvailabilityRuleRequest(5, LocalTime.of(9, 0), LocalTime.of(12, 30))));

        assertThat(week).extracting(AvailabilityRuleResponse::startTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(15, 0));
        assertThat(ruleRepository.count()).isEqualTo(2);
        assertThat(service.getRules()).isEqualTo(week);
    }

    @Test
    void blocksAreListedByDate() {
        LocalDate later = LocalDate.now().plusYears(5);
        service.createBlock(new AvailabilityBlockRequest(later.plusDays(1), later.plusDays(1), null, null, "Zzferiado"));
        service.createBlock(new AvailabilityBlockRequest(later, later, LocalTime.of(15, 0), LocalTime.of(16, 0), null));

        assertThat(service.listBlocks(later, later.plusDays(1)))
                .extracting(block -> block.startDate() + " " + block.startTime())
                .containsExactly(later + " 15:00", later.plusDays(1) + " null");
    }

    @Test
    void aSeveralDayBlockShowsUpOnAnyOfItsDays() {
        LocalDate later = LocalDate.now().plusYears(5);
        service.createBlock(new AvailabilityBlockRequest(later, later.plusDays(14), null, null, "Zzvacaciones"));

        assertThat(service.listBlocks(later.plusDays(7), later.plusDays(7))).hasSize(1);
        assertThat(service.listBlocks(later.plusDays(15), later.plusDays(20))).isEmpty();
        assertThat(service.listBlocks(later.plusDays(14), null)).hasSize(1);
    }

    @Test
    void theDatabaseRejectsABlockWithOnlyOneHour() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO availability_block (start_date, end_date, start_time) VALUES ('2030-01-01', '2030-01-01', '10:00')"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_availability_block_hours");
    }

    @Test
    void theDatabaseRejectsHoursOnASeveralDayBlock() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO availability_block (start_date, end_date, start_time, end_time) "
                + "VALUES ('2030-01-01', '2030-01-02', '10:00', '11:00')"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_availability_block_hours");
    }

    @Test
    void theDatabaseRejectsADayOutsideTheWeek() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO availability_rule (day_of_week, start_time, end_time) VALUES (0, '09:00', '10:00')"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_availability_rule_day");
    }
}
