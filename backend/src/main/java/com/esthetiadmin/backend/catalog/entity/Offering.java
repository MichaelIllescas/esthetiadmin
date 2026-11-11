package com.esthetiadmin.backend.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** A service the center offers (table {@code service}); named Offering to avoid clashing with Spring's @Service. */
@Entity
@Table(name = "service")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Offering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active = true;

    /** Whether it is offered on the public booking page (ADR 014). */
    @Column(nullable = false)
    private boolean bookable = true;

    public Offering(Long categoryId, String name, String description, BigDecimal price, boolean bookable) {
        update(categoryId, name, description, price, bookable);
    }

    public final void update(Long categoryId, String name, String description, BigDecimal price, boolean bookable) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.bookable = bookable;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }
}
