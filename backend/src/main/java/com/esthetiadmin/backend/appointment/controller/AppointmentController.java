package com.esthetiadmin.backend.appointment.controller;

import com.esthetiadmin.backend.appointment.dto.AppointmentClientRequest;
import com.esthetiadmin.backend.appointment.dto.AppointmentNoticeRequest;
import com.esthetiadmin.backend.appointment.dto.AppointmentRequest;
import com.esthetiadmin.backend.appointment.dto.AppointmentResponse;
import com.esthetiadmin.backend.appointment.dto.AppointmentStatusRequest;
import com.esthetiadmin.backend.appointment.dto.DayAvailabilityResponse;
import com.esthetiadmin.backend.appointment.entity.AppointmentStatus;
import com.esthetiadmin.backend.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController implements AppointmentControllerDocs {

    private final AppointmentService appointmentService;

    @Override
    @GetMapping
    public List<AppointmentResponse> search(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) AppointmentStatus status) {
        return appointmentService.search(from, to, status);
    }

    @Override
    @GetMapping("/availability")
    public DayAvailabilityResponse availability(
            @RequestParam Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long excludeAppointmentId) {
        return appointmentService.availability(serviceId, date, excludeAppointmentId);
    }

    @Override
    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id) {
        return appointmentService.get(id);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(@Valid @RequestBody AppointmentRequest request) {
        return appointmentService.create(request);
    }

    @Override
    @PutMapping("/{id}")
    public AppointmentResponse update(@PathVariable Long id, @Valid @RequestBody AppointmentRequest request) {
        return appointmentService.update(id, request);
    }

    @Override
    @PatchMapping("/{id}/status")
    public AppointmentResponse changeStatus(@PathVariable Long id,
                                            @Valid @RequestBody AppointmentStatusRequest request) {
        return appointmentService.changeStatus(id, request.status());
    }

    @Override
    @PatchMapping("/{id}/client")
    public AppointmentResponse linkClient(@PathVariable Long id, @Valid @RequestBody AppointmentClientRequest request) {
        return appointmentService.linkClient(id, request.clientId());
    }

    @Override
    @PostMapping("/{id}/notifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendNotice(@PathVariable Long id, @Valid @RequestBody AppointmentNoticeRequest request) {
        appointmentService.sendNotice(id, request.type());
    }
}
