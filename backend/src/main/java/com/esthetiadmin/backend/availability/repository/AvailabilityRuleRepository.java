package com.esthetiadmin.backend.availability.repository;

import com.esthetiadmin.backend.availability.entity.AvailabilityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, Long> {

    List<AvailabilityRule> findAllByOrderByDayOfWeekAscStartTimeAsc();
}
