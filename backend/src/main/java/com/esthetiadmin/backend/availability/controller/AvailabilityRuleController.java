package com.esthetiadmin.backend.availability.controller;

import com.esthetiadmin.backend.availability.dto.AvailabilityRuleRequest;
import com.esthetiadmin.backend.availability.dto.AvailabilityRuleResponse;
import com.esthetiadmin.backend.availability.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/availability-rules")
@RequiredArgsConstructor
public class AvailabilityRuleController implements AvailabilityRuleControllerDocs {

    private final AvailabilityService availabilityService;

    @Override
    @GetMapping
    public List<AvailabilityRuleResponse> findAll() {
        return availabilityService.getRules();
    }

    @Override
    @PutMapping
    public List<AvailabilityRuleResponse> replaceAll(@RequestBody List<AvailabilityRuleRequest> request) {
        return availabilityService.replaceRules(request);
    }
}
