package com.esthetiadmin.backend.catalog.controller;

import com.esthetiadmin.backend.catalog.dto.ActiveRequest;
import com.esthetiadmin.backend.catalog.dto.OfferingRequest;
import com.esthetiadmin.backend.catalog.dto.OfferingResponse;
import com.esthetiadmin.backend.catalog.service.OfferingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class OfferingController implements OfferingControllerDocs {

    private final OfferingService offeringService;

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OfferingResponse create(@Valid @RequestBody OfferingRequest request) {
        return offeringService.create(request);
    }

    @Override
    @PutMapping("/{id}")
    public OfferingResponse update(@PathVariable Long id, @Valid @RequestBody OfferingRequest request) {
        return offeringService.update(id, request);
    }

    @Override
    @PatchMapping("/{id}/active")
    public OfferingResponse setActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return offeringService.setActive(id, request.active());
    }

    @Override
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        offeringService.delete(id);
    }
}
