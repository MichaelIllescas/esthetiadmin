package com.esthetiadmin.backend.appointment.controller;

import com.esthetiadmin.backend.appointment.dto.AvailableDaysResponse;
import com.esthetiadmin.backend.appointment.dto.DayAvailabilityResponse;
import com.esthetiadmin.backend.appointment.dto.PublicBookingRequest;
import com.esthetiadmin.backend.appointment.dto.PublicBookingResponse;
import com.esthetiadmin.backend.appointment.service.PublicBookingService;
import com.esthetiadmin.backend.catalog.dto.BookableCategory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** The only routes open without login besides the login itself (ADR 014). */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicBookingController implements PublicBookingControllerDocs {

    private final PublicBookingService bookingService;

    @Override
    @GetMapping("/services")
    public List<BookableCategory> services() {
        return bookingService.services();
    }

    @Override
    @GetMapping("/availability/days")
    public AvailableDaysResponse availableDays(@RequestParam Long serviceId,
                                               @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return bookingService.availableDays(serviceId, month);
    }

    @Override
    @GetMapping("/availability")
    public DayAvailabilityResponse availability(@RequestParam Long serviceId,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.availability(serviceId, date);
    }

    @Override
    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicBookingResponse book(@Valid @RequestBody PublicBookingRequest request, HttpServletRequest httpRequest) {
        return bookingService.book(request, httpRequest.getRemoteAddr());
    }
}
