package com.esthetiadmin.backend.treatment.repository;

import com.esthetiadmin.backend.catalog.dto.CategoryRequest;
import com.esthetiadmin.backend.catalog.dto.OfferingRequest;
import com.esthetiadmin.backend.catalog.dto.OfferingResponse;
import com.esthetiadmin.backend.catalog.service.CategoryService;
import com.esthetiadmin.backend.catalog.service.OfferingService;
import com.esthetiadmin.backend.client.dto.ClientRequest;
import com.esthetiadmin.backend.client.dto.ClientResponse;
import com.esthetiadmin.backend.client.service.ClientService;
import com.esthetiadmin.backend.common.dto.PageResponse;
import com.esthetiadmin.backend.common.exception.ConflictException;
import com.esthetiadmin.backend.treatment.dto.TreatmentRequest;
import com.esthetiadmin.backend.treatment.dto.TreatmentResponse;
import com.esthetiadmin.backend.treatment.dto.TreatmentUpdateRequest;
import com.esthetiadmin.backend.treatment.service.TreatmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the treatment rules end to end against the real database, together with the catalog and the clients.
 * Everything happens in a transaction that is rolled back, so no data is left behind.
 */
@SpringBootTest
@Transactional
class TreatmentDatabaseTest {

    @Autowired private TreatmentService treatments;
    @Autowired private CategoryService categories;
    @Autowired private OfferingService offerings;
    @Autowired private ClientService clients;

    private Long lasersId;
    private Long facialsId;
    private OfferingResponse bozo;
    private OfferingResponse armpits;
    private OfferingResponse cleaning;
    private ClientResponse carina;
    private ClientResponse rosa;

    @BeforeEach
    void setUp() {
        lasersId = categories.create(new CategoryRequest("Zzlaser", null)).id();
        facialsId = categories.create(new CategoryRequest("Zzfacial", null)).id();
        bozo = offerings.create(new OfferingRequest(lasersId, "Zzbozo", null, new BigDecimal("15000"), null));
        armpits = offerings.create(new OfferingRequest(lasersId, "Zzaxilas", null, new BigDecimal("18000"), null));
        cleaning = offerings.create(new OfferingRequest(facialsId, "Zzlimpieza", null, new BigDecimal("25000"), null));
        carina = clients.create(new ClientRequest("Zzcarina", "Zzdiaz", null, null, null, null));
        rosa = clients.create(new ClientRequest("Zzrosa", "Zzfunes", null, null, null, null));
    }

    private TreatmentResponse register(ClientResponse client, OfferingResponse offering, LocalDate day, String amount) {
        return treatments.create(new TreatmentRequest(client.id(), offering.id(), day, new BigDecimal(amount), null));
    }

    private PageResponse<TreatmentResponse> search(Long clientId, Long serviceId, Long categoryId, LocalDate from, LocalDate to) {
        return treatments.search(clientId, serviceId, categoryId, from, to, 0, 50);
    }

    @Test
    void registersAtreatmentAndReadsItBackWithNames() {
        TreatmentResponse created = register(carina, bozo, LocalDate.of(2026, 9, 25), "15000");

        var history = search(carina.id(), null, null, null, null);

        assertThat(history.content()).hasSize(1);
        assertThat(history.content().get(0).id()).isEqualTo(created.id());
        assertThat(history.content().get(0).clientName()).isEqualTo("Zzdiaz, Zzcarina");
        assertThat(history.content().get(0).serviceName()).isEqualTo("Zzbozo");
        assertThat(history.content().get(0).categoryName()).isEqualTo("Zzlaser");
        assertThat(history.content().get(0).amount()).isEqualByComparingTo("15000");
    }

    @Test
    void aFreeTreatmentIsAccepted() {
        assertThat(register(carina, bozo, LocalDate.of(2026, 9, 25), "0").amount()).isEqualByComparingTo("0");
    }

    @Test
    void theHistoryComesNewestFirstAndOnlyOfTheChosenClient() {
        register(carina, bozo, LocalDate.of(2026, 9, 1), "100");
        register(carina, armpits, LocalDate.of(2026, 9, 20), "200");
        register(rosa, bozo, LocalDate.of(2026, 9, 25), "300");
        register(carina, cleaning, LocalDate.of(2026, 9, 10), "400");

        var history = search(carina.id(), null, null, null, null);

        assertThat(history.content()).extracting(t -> t.amount().intValue()).containsExactly(200, 400, 100);
    }

    @Test
    void filtersByServiceAndByCategory() {
        register(carina, bozo, LocalDate.of(2026, 9, 1), "100");
        register(carina, armpits, LocalDate.of(2026, 9, 2), "200");
        register(carina, cleaning, LocalDate.of(2026, 9, 3), "400");

        assertThat(search(carina.id(), bozo.id(), null, null, null).content()).extracting(TreatmentResponse::serviceName)
                .containsExactly("Zzbozo");
        assertThat(search(carina.id(), null, lasersId, null, null).content()).extracting(TreatmentResponse::serviceName)
                .containsExactlyInAnyOrder("Zzbozo", "Zzaxilas");
        assertThat(search(carina.id(), cleaning.id(), lasersId, null, null).content()).isEmpty();
        assertThat(search(carina.id(), null, facialsId, null, null).content()).extracting(TreatmentResponse::serviceName)
                .containsExactly("Zzlimpieza");
    }

    @Test
    void theDateRangeIncludesBothEnds() {
        register(carina, bozo, LocalDate.of(2026, 9, 1), "100");
        register(carina, bozo, LocalDate.of(2026, 9, 15), "200");
        register(carina, bozo, LocalDate.of(2026, 9, 30), "300");
        register(carina, bozo, LocalDate.of(2026, 10, 1), "400");

        assertThat(search(carina.id(), null, null, LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 30)).content())
                .extracting(t -> t.amount().intValue()).containsExactly(300, 200);
        assertThat(search(carina.id(), null, null, LocalDate.of(2026, 9, 30), null).content())
                .extracting(t -> t.amount().intValue()).containsExactly(400, 300);
        assertThat(search(carina.id(), null, null, null, LocalDate.of(2026, 9, 1)).content())
                .extracting(t -> t.amount().intValue()).containsExactly(100);
    }

    @Test
    void paginatesTheHistory() {
        for (int day = 1; day <= 5; day++) {
            register(carina, bozo, LocalDate.of(2026, 9, day), String.valueOf(day * 100));
        }

        var second = treatments.search(carina.id(), null, null, null, null, 1, 2);

        assertThat(second.totalElements()).isEqualTo(5);
        assertThat(second.totalPages()).isEqualTo(3);
        assertThat(second.content()).extracting(t -> t.amount().intValue()).containsExactly(300, 200);
    }

    @Test
    void editingChangesTheDataAndTheAmountKeepsItsCents() {
        TreatmentResponse created = register(carina, bozo, LocalDate.of(2026, 9, 25), "15000");

        TreatmentResponse edited = treatments.update(created.id(),
                new TreatmentUpdateRequest(cleaning.id(), LocalDate.of(2026, 9, 24), new BigDecimal("22500.50"), "Con descuento"));

        assertThat(edited.clientId()).isEqualTo(carina.id());
        assertThat(edited.serviceName()).isEqualTo("Zzlimpieza");
        assertThat(edited.amount()).isEqualByComparingTo("22500.50");
        assertThat(edited.notes()).isEqualTo("Con descuento");
    }

    @Test
    void aDeactivatedServiceOrCategoryCannotBeUsedForANewTreatment() {
        offerings.setActive(armpits.id(), false);
        categories.setActive(facialsId, false);

        assertThatThrownBy(() -> register(carina, armpits, LocalDate.of(2026, 9, 25), "100"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> register(carina, cleaning, LocalDate.of(2026, 9, 25), "100"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void aTreatmentKeepsItsServiceEvenAfterTheServiceIsDeactivated() {
        TreatmentResponse created = register(carina, armpits, LocalDate.of(2026, 9, 25), "100");
        offerings.setActive(armpits.id(), false);

        TreatmentResponse edited = treatments.update(created.id(),
                new TreatmentUpdateRequest(armpits.id(), LocalDate.of(2026, 9, 25), new BigDecimal("150"), null));

        assertThat(edited.amount()).isEqualByComparingTo("150");
        assertThat(edited.serviceName()).isEqualTo("Zzaxilas");
    }

    @Test
    void aClientWithTreatmentsCannotBeDeletedUntilTheyAreRemoved() {
        TreatmentResponse created = register(carina, bozo, LocalDate.of(2026, 9, 25), "100");

        assertThatThrownBy(() -> clients.delete(carina.id())).isInstanceOf(ConflictException.class);

        treatments.delete(created.id());
        clients.delete(carina.id());
        assertThat(clients.existsWithName("Zzcarina", "Zzdiaz", null)).isFalse();
    }

    @Test
    void aServiceWithTreatmentsCannotBeDeletedButOneWithoutCan() {
        register(carina, bozo, LocalDate.of(2026, 9, 25), "100");

        assertThatThrownBy(() -> offerings.delete(bozo.id())).isInstanceOf(ConflictException.class);
        offerings.delete(cleaning.id());
    }
}
