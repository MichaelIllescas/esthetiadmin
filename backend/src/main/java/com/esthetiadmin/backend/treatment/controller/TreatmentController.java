package com.esthetiadmin.backend.treatment.controller;

import com.esthetiadmin.backend.common.dto.PageResponse;
import com.esthetiadmin.backend.treatment.dto.TreatmentRequest;
import com.esthetiadmin.backend.treatment.dto.TreatmentResponse;
import com.esthetiadmin.backend.treatment.dto.TreatmentUpdateRequest;
import com.esthetiadmin.backend.treatment.service.TreatmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/treatments")
@RequiredArgsConstructor
public class TreatmentController implements TreatmentControllerDocs {

    private final TreatmentService treatmentService;

    @Override
    @GetMapping
    public PageResponse<TreatmentResponse> search(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return treatmentService.search(clientId, serviceId, categoryId, from, to, page, size);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TreatmentResponse create(@RequestBody TreatmentRequest request) {
        return treatmentService.create(request);
    }

    @Override
    @PutMapping("/{id}")
    public TreatmentResponse update(@PathVariable Long id, @RequestBody TreatmentUpdateRequest request) {
        return treatmentService.update(id, request);
    }

    @Override
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        treatmentService.delete(id);
    }
}
