package com.esthetiadmin.backend.availability.controller;

import com.esthetiadmin.backend.availability.dto.AvailabilityBlockRequest;
import com.esthetiadmin.backend.availability.dto.AvailabilityBlockResponse;
import com.esthetiadmin.backend.availability.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/availability-blocks")
@RequiredArgsConstructor
public class AvailabilityBlockController implements AvailabilityBlockControllerDocs {

    private final AvailabilityService availabilityService;

    @Override
    @GetMapping
    public List<AvailabilityBlockResponse> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return availabilityService.listBlocks(from, to);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityBlockResponse create(@Valid @RequestBody AvailabilityBlockRequest request) {
        return availabilityService.createBlock(request);
    }

    @Override
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        availabilityService.deleteBlock(id);
    }
}
