package com.esthetiadmin.backend.availability.service;

import com.esthetiadmin.backend.availability.dto.AffectedAppointmentResponse;
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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** The owner's weekly opening hours and the dates she takes out of the schedule (ADR 013). */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityService.class);
    private static final int GRID_MINUTES = 30;
    private static final int MAX_RULES = 50;
    private static final int MAX_BLOCK_DAYS = 366;
    private static final String[] DAY_NAMES = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};

    private final AvailabilityRuleRepository ruleRepository;
    private final AvailabilityBlockRepository blockRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AvailabilityRuleResponse> getRules() {
        return ruleRepository.findAllByOrderByDayOfWeekAscStartTimeAsc().stream()
                .map(AvailabilityRuleResponse::from)
                .toList();
    }

    /** Replaces the whole week: the request is the complete schedule, an empty list closes every day. */
    @Transactional
    public List<AvailabilityRuleResponse> replaceRules(List<AvailabilityRuleRequest> requests) {
        if (requests == null) {
            throw new BadRequestException("Indicá los horarios de atención");
        }
        if (requests.size() > MAX_RULES) {
            throw new BadRequestException("No se pueden cargar más de " + MAX_RULES + " rangos de atención");
        }
        requests.forEach(this::validateRule);
        ensureNoOverlapWithinADay(requests);

        ruleRepository.deleteAllInBatch();
        List<AvailabilityRule> saved = ruleRepository.saveAll(requests.stream()
                .map(request -> new AvailabilityRule(request.dayOfWeek(), request.startTime(), request.endTime()))
                .toList());
        log.info("Opening hours replaced: ranges={}", saved.size());
        return saved.stream()
                .sorted(Comparator.comparingInt(AvailabilityRule::getDayOfWeek)
                        .thenComparing(AvailabilityRule::getStartTime))
                .map(AvailabilityRuleResponse::from)
                .toList();
    }

    /** Blocks sharing a day with {@code from} (today when missing) .. {@code to} (no limit when missing). */
    @Transactional(readOnly = true)
    public List<AvailabilityBlockResponse> listBlocks(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now(clock);
        if (to != null && start.isAfter(to)) {
            throw new BadRequestException("La fecha \"desde\" no puede ser posterior a la fecha \"hasta\"");
        }
        List<AvailabilityBlock> blocks = to == null
                ? blockRepository.findAllByEndDateGreaterThanEqualOrderByStartDateAscStartTimeAsc(start)
                : blockRepository.findAllByEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDateAscStartTimeAsc(
                        start, to);
        return blocks.stream().map(block -> AvailabilityBlockResponse.from(block, List.of())).toList();
    }

    @Transactional
    public AvailabilityBlockResponse createBlock(AvailabilityBlockRequest request) {
        if (request.startDate().isBefore(LocalDate.now(clock))) {
            throw new BadRequestException("No se puede bloquear una fecha pasada");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("La fecha de fin del bloqueo no puede ser anterior a la de inicio");
        }
        if (request.startDate().plusDays(MAX_BLOCK_DAYS).isBefore(request.endDate())) {
            throw new BadRequestException("Un bloqueo no puede durar más de " + MAX_BLOCK_DAYS + " días");
        }
        if ((request.startTime() == null) != (request.endTime() == null)) {
            throw new BadRequestException("Indicá las dos horas del bloqueo, o ninguna para bloquear el día completo");
        }
        if (request.startTime() != null) {
            if (!request.startDate().equals(request.endDate())) {
                throw new BadRequestException("Un bloqueo de varios días tiene que ser por días completos");
            }
            validateRange(request.startTime(), request.endTime());
        }
        AvailabilityBlock saved = blockRepository.save(new AvailabilityBlock(request.startDate(), request.endDate(),
                request.startTime(), request.endTime(), cleanReason(request.reason())));
        log.info("Schedule block created: blockId={}, from={}, to={}, wholeDay={}", saved.getId(),
                saved.getStartDate(), saved.getEndDate(), saved.isWholeDay());
        return AvailabilityBlockResponse.from(saved, affectedBy(saved));
    }

    /** The block does not cancel them: they are returned so the owner can decide what to do with each one. */
    private List<AffectedAppointmentResponse> affectedBy(AvailabilityBlock block) {
        LocalDateTime from = block.isWholeDay()
                ? block.getStartDate().atStartOfDay()
                : block.getStartDate().atTime(block.getStartTime());
        LocalDateTime to = block.isWholeDay()
                ? block.getEndDate().plusDays(1).atStartOfDay()
                : block.getEndDate().atTime(block.getEndTime());
        return blockRepository.findConfirmedAppointmentsBetween(from, to).stream()
                .map(view -> new AffectedAppointmentResponse(view.getId(), view.getStartsAt(), view.getFirstName(),
                        view.getLastName()))
                .toList();
    }

    @Transactional
    public void deleteBlock(Long id) {
        AvailabilityBlock block = blockRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El bloqueo no existe"));
        blockRepository.delete(block);
        log.info("Schedule block deleted: blockId={}", id);
    }

    private void validateRule(AvailabilityRuleRequest request) {
        if (request == null || request.dayOfWeek() == null || request.startTime() == null
                || request.endTime() == null) {
            throw new BadRequestException("Cada rango necesita el día, la hora de inicio y la hora de fin");
        }
        if (request.dayOfWeek() < 1 || request.dayOfWeek() > 7) {
            throw new BadRequestException("El día de la semana tiene que ir de 1 (lunes) a 7 (domingo)");
        }
        validateRange(request.startTime(), request.endTime());
    }

    private void validateRange(LocalTime start, LocalTime end) {
        if (!onGrid(start) || !onGrid(end)) {
            throw new BadRequestException("Las horas tienen que ser en punto o y media (por ejemplo 9:00 o 9:30)");
        }
        if (!start.isBefore(end)) {
            throw new BadRequestException("La hora de inicio tiene que ser anterior a la de fin");
        }
    }

    private boolean onGrid(LocalTime time) {
        return time.getSecond() == 0 && time.getNano() == 0 && time.getMinute() % GRID_MINUTES == 0;
    }

    private void ensureNoOverlapWithinADay(List<AvailabilityRuleRequest> requests) {
        Map<Integer, List<AvailabilityRuleRequest>> byDay = requests.stream()
                .collect(Collectors.groupingBy(AvailabilityRuleRequest::dayOfWeek));
        byDay.forEach((day, ranges) -> {
            List<AvailabilityRuleRequest> sorted = ranges.stream()
                    .sorted(Comparator.comparing(AvailabilityRuleRequest::startTime))
                    .toList();
            for (int i = 1; i < sorted.size(); i++) {
                if (sorted.get(i).startTime().isBefore(sorted.get(i - 1).endTime())) {
                    throw new BadRequestException("Los rangos del " + DAY_NAMES[day - 1] + " se superponen");
                }
            }
        });
    }

    /** An empty reason is stored as no reason. */
    private String cleanReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }
}
