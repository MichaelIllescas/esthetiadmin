package com.esthetiadmin.backend.catalog.dto;

/**
 * What other modules may know about a service: its names, whether it can be offered today, whether it is offered on
 * the public booking page and how long its appointments last (set by its category).
 */
public record OfferingInfo(Long id, String name, Long categoryId, String categoryName, boolean offerable,
                           boolean bookable, int appointmentMinutes) {
}
