package com.esthetiadmin.backend.availability.service;

import com.esthetiadmin.backend.availability.dto.AvailabilityBlockRequest;
import com.esthetiadmin.backend.availability.dto.AvailabilityBlockResponse;
import com.esthetiadmin.backend.availability.dto.AvailabilityRuleRequest;
import com.esthetiadmin.backend.availability.dto.AvailabilityRuleResponse;
import com.esthetiadmin.backend.availability.entity.AvailabilityBlock;
import com.esthetiadmin.backend.availability.entity.AvailabilityRule;
import com.esthetiadmin.backend.availability.repository.AvailabilityBlockRepository;
import com.esthetiadmin.backend.availability.repository.AvailabilityRuleRepository;
import com.esthetiadmin.backend.common.exception.BadRequestException;
import com.esthetiadmin.backend.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvailabilityServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);

    private AvailabilityRuleRepository ruleRepository;
    private AvailabilityBlockRepository blockRepository;
    private AvailabilityService service;

    @BeforeEach
    void setUp() {
        ruleRepository = mock(AvailabilityRuleRepository.class);
        blockRepository = mock(AvailabilityBlockRepository.class);
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new AvailabilityService(ruleRepository, blockRepository, clock);
        when(ruleRepository.saveAll(anyList())).thenAnswer(call -> new ArrayList<>(call.<List<?>>getArgument(0)));
        when(blockRepository.save(any(AvailabilityBlock.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void replaceRulesSwapsTheWholeWeekAndReturnsItSorted() {
        List<AvailabilityRuleResponse> result = service.replaceRules(List.of(
                rule(2, "09:00", "13:00"), rule(1, "15:00", "19:00"), rule(1, "09:00", "13:00")));

        verify(ruleRepository).deleteAllInBatch();
        ArgumentCaptor<List<AvailabilityRule>> saved = ArgumentCaptor.forClass(List.class);
        verify(ruleRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(3);
        assertThat(result).extracting(r -> r.dayOfWeek() + " " + r.startTime())
                .containsExactly("1 09:00", "1 15:00", "2 09:00");
    }

    @Test
    void anEmptyWeekClosesEveryDay() {
        assertThat(service.replaceRules(List.of())).isEmpty();
        verify(ruleRepository).deleteAllInBatch();
    }

    @Test
    void rangesThatTouchAreNotAnOverlap() {
        assertThat(service.replaceRules(List.of(rule(1, "09:00", "13:00"), rule(1, "13:00", "15:00")))).hasSize(2);
    }

    @Test
    void replaceRulesRejectsOverlappingRangesOfTheSameDay() {
        assertRuleRejected(List.of(rule(3, "09:00", "13:00"), rule(3, "12:30", "15:00")),
                "Los rangos del miércoles se superponen");
    }

    @Test
    void rangesOfDifferentDaysCanShareHours() {
        assertThat(service.replaceRules(List.of(rule(1, "09:00", "13:00"), rule(2, "09:00", "13:00")))).hasSize(2);
    }

    @Test
    void replaceRulesRejectsHoursOutsideTheHalfHourGrid() {
        assertRuleRejected(List.of(rule(1, "09:15", "13:00")), "Las horas tienen que ser en punto o y media");
    }

    @Test
    void replaceRulesRejectsAStartThatIsNotBeforeTheEnd() {
        assertRuleRejected(List.of(rule(1, "13:00", "13:00")), "La hora de inicio tiene que ser anterior a la de fin");
    }

    @Test
    void replaceRulesRejectsADayOutsideTheWeek() {
        assertRuleRejected(List.of(rule(8, "09:00", "13:00")), "El día de la semana tiene que ir de 1 (lunes) a 7");
    }

    @Test
    void replaceRulesRejectsIncompleteRanges() {
        assertRuleRejected(List.of(new AvailabilityRuleRequest(1, null, LocalTime.of(13, 0))),
                "Cada rango necesita el día");
        assertRuleRejected(Arrays.asList((AvailabilityRuleRequest) null), "Cada rango necesita el día");
        assertRuleRejected(null, "Indicá los horarios de atención");
    }

    @Test
    void createBlockForAWholeDayWithoutReason() {
        AvailabilityBlockResponse response = service.createBlock(
                new AvailabilityBlockRequest(TODAY.plusDays(9), TODAY.plusDays(9), null, null, "   "));

        assertThat(response.startDate()).isEqualTo(TODAY.plusDays(9));
        assertThat(response.endDate()).isEqualTo(TODAY.plusDays(9));
        assertThat(response.startTime()).isNull();
        assertThat(response.reason()).isNull();
        assertThat(response.affectedAppointments()).isEmpty();
    }

    @Test
    void createBlockForARangeOfToday() {
        AvailabilityBlockResponse response = service.createBlock(
                new AvailabilityBlockRequest(TODAY, TODAY, LocalTime.of(15, 0), LocalTime.of(19, 0), " Trámite "));

        assertThat(response.startTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(response.reason()).isEqualTo("Trámite");
    }

    @Test
    void createBlockCoversSeveralWholeDays() {
        AvailabilityBlockResponse response = service.createBlock(
                new AvailabilityBlockRequest(TODAY.plusDays(90), TODAY.plusDays(104), null, null, "Vacaciones"));

        assertThat(response.startDate()).isEqualTo(TODAY.plusDays(90));
        assertThat(response.endDate()).isEqualTo(TODAY.plusDays(104));
    }

    @Test
    void createBlockRejectsAnEndBeforeTheStart() {
        assertBlockRejected(new AvailabilityBlockRequest(TODAY.plusDays(5), TODAY.plusDays(4), null, null, null),
                "La fecha de fin del bloqueo no puede ser anterior a la de inicio");
    }

    @Test
    void createBlockRejectsMoreThanAYear() {
        assertBlockRejected(new AvailabilityBlockRequest(TODAY, TODAY.plusDays(367), null, null, null),
                "Un bloqueo no puede durar más de 366 días");
    }

    @Test
    void createBlockOnlyAcceptsHoursOnASingleDay() {
        assertBlockRejected(
                new AvailabilityBlockRequest(TODAY, TODAY.plusDays(2), LocalTime.of(15, 0), LocalTime.of(19, 0), null),
                "Un bloqueo de varios días tiene que ser por días completos");
    }

    @Test
    void createBlockRejectsAPastDate() {
        assertBlockRejected(new AvailabilityBlockRequest(TODAY.minusDays(1), TODAY, null, null, null),
                "No se puede bloquear una fecha pasada");
    }

    @Test
    void createBlockNeedsBothHoursOrNone() {
        assertBlockRejected(new AvailabilityBlockRequest(TODAY, TODAY, LocalTime.of(15, 0), null, null),
                "Indicá las dos horas del bloqueo");
    }

    @Test
    void createBlockRejectsHoursOutsideTheGrid() {
        assertBlockRejected(new AvailabilityBlockRequest(TODAY, TODAY, LocalTime.of(15, 10), LocalTime.of(16, 0), null),
                "Las horas tienen que ser en punto o y media");
    }

    @Test
    void listBlocksStartsTodayWhenNoDateIsGiven() {
        when(blockRepository.findAllByEndDateGreaterThanEqualOrderByStartDateAscStartTimeAsc(TODAY))
                .thenReturn(List.of(new AvailabilityBlock(TODAY, TODAY, null, null, "Feriado")));

        assertThat(service.listBlocks(null, null)).extracting(AvailabilityBlockResponse::reason)
                .containsExactly("Feriado");
    }

    @Test
    void listBlocksUsesTheGivenRange() {
        LocalDate to = TODAY.plusDays(30);
        when(blockRepository.findAllByEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDateAscStartTimeAsc(TODAY, to)).thenReturn(List.of());

        assertThat(service.listBlocks(TODAY, to)).isEmpty();
    }

    @Test
    void listBlocksRejectsAnInvertedRange() {
        assertThatThrownBy(() -> service.listBlocks(TODAY, TODAY.minusDays(1)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteBlockRemovesIt() {
        AvailabilityBlock block = new AvailabilityBlock(TODAY, TODAY, null, null, null);
        when(blockRepository.findById(3L)).thenReturn(Optional.of(block));

        service.deleteBlock(3L);

        verify(blockRepository).delete(block);
    }

    @Test
    void deleteBlockFailsWhenItDoesNotExist() {
        when(blockRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteBlock(3L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("El bloqueo no existe");
    }

    private void assertRuleRejected(List<AvailabilityRuleRequest> requests, String message) {
        assertThatThrownBy(() -> service.replaceRules(requests))
                .isInstanceOf(BadRequestException.class)
                .hasMessageStartingWith(message);
        verify(ruleRepository, never()).deleteAllInBatch();
    }

    private void assertBlockRejected(AvailabilityBlockRequest request, String message) {
        assertThatThrownBy(() -> service.createBlock(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageStartingWith(message);
        verify(blockRepository, never()).save(any());
    }

    private AvailabilityRuleRequest rule(int day, String start, String end) {
        return new AvailabilityRuleRequest(day, LocalTime.parse(start), LocalTime.parse(end));
    }
}
