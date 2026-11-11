package com.esthetiadmin.backend.client.repository;

import com.esthetiadmin.backend.catalog.repository.OfferingRepository;
import com.esthetiadmin.backend.client.entity.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against the real database to check what mocks cannot: the collation-based search and the native
 * queries. Everything happens in a transaction that is rolled back, so no data is left behind.
 */
@SpringBootTest
@Transactional
class ClientDatabaseTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OfferingRepository offeringRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private List<String> search(String text) {
        return clientRepository
                .findAll(ClientSpecifications.matching(text), PageRequest.of(0, 50, Sort.by("lastName", "firstName")))
                .map(client -> client.getFirstName() + " " + client.getLastName())
                .getContent();
    }

    private void saveClients() {
        clientRepository.saveAll(List.of(
                new Client("Zzcarina", "Zzdíaz", null, null, null),
                new Client("Zzmaría", "Zzpérez", null, null, null),
                new Client("Zzana", "Zzpérez", null, null, null),
                new Client("Zz100%", "Zzsuárez", null, null, null)));
        clientRepository.flush();
    }

    @Test
    void searchIgnoresCaseAndAccents() {
        saveClients();

        assertThat(search("ZZCARINA")).containsExactly("Zzcarina Zzdíaz");
        assertThat(search("zzdiaz")).containsExactly("Zzcarina Zzdíaz");
        assertThat(search("zzmaria")).containsExactly("Zzmaría Zzpérez");
    }

    @Test
    void searchRequiresEveryWordAndLooksInBothNames() {
        saveClients();

        assertThat(search("zzana zzperez")).containsExactly("Zzana Zzpérez");
        assertThat(search("zzperez zzana")).containsExactly("Zzana Zzpérez");
        assertThat(search("zzperez")).containsExactly("Zzana Zzpérez", "Zzmaría Zzpérez");
        assertThat(search("zzana zzsuarez")).isEmpty();
    }

    @Test
    void searchTreatsWildcardsAsPlainText() {
        saveClients();

        assertThat(search("zz100%")).containsExactly("Zz100% Zzsuárez");
        assertThat(search("zz%")).isEmpty();
        assertThat(search("zz_ana")).isEmpty();
    }

    @Test
    void aBlankSearchListsEveryone() {
        saveClients();

        assertThat(search("  ")).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void homonymCheckIgnoresCaseAndAccentsAndCanExcludeOneClient() {
        saveClients();
        Client carina = clientRepository.findAll().stream()
                .filter(client -> client.getFirstName().equals("Zzcarina")).findFirst().orElseThrow();

        assertThat(clientRepository.existsByFirstNameAndLastName("ZZCARINA", "zzdiaz")).isTrue();
        assertThat(clientRepository.existsByFirstNameAndLastNameAndIdNot("Zzcarina", "Zzdíaz", carina.getId())).isFalse();
        assertThat(clientRepository.existsByFirstNameAndLastName("Zzcarina", "Otro")).isFalse();
    }

    @Test
    void birthdayQueryOnlyReturnsClientsWithABirthDate() {
        clientRepository.save(new Client("Zzcon", "Zzfecha", null, LocalDate.of(1990, 5, 5), null));
        clientRepository.save(new Client("Zzsin", "Zzfecha", null, null, null));
        clientRepository.flush();

        assertThat(clientRepository.findAllByBirthDateIsNotNull())
                .extracting(Client::getFirstName).contains("Zzcon").doesNotContain("Zzsin");
    }

    @Test
    void countsTheTreatmentsOfAClientAndOfAService() {
        Client client = clientRepository.saveAndFlush(new Client("Zzcliente", "Zzconhistorial", null, null, null));
        Client withoutTreatments = clientRepository.saveAndFlush(new Client("Zzcliente", "Zzsinhistorial", null, null, null));
        jdbc.update("INSERT INTO category (name) VALUES ('Zzcategoria')");
        Long categoryId = jdbc.queryForObject("SELECT id FROM category WHERE name = 'Zzcategoria'", Long.class);
        jdbc.update("INSERT INTO service (category_id, name, price) VALUES (?, 'Zzservicio', 100)", categoryId);
        Long serviceId = jdbc.queryForObject("SELECT id FROM service WHERE name = 'Zzservicio'", Long.class);
        Long unusedServiceId = jdbc.queryForObject("SELECT COUNT(*) FROM service", Long.class) + 1_000_000;
        jdbc.update("INSERT INTO treatment (client_id, service_id, performed_on, amount) VALUES (?, ?, '2026-09-01', 100)",
                client.getId(), serviceId);
        jdbc.update("INSERT INTO treatment (client_id, service_id, performed_on, amount) VALUES (?, ?, '2026-09-02', 100)",
                client.getId(), serviceId);

        assertThat(clientRepository.countTreatments(client.getId())).isEqualTo(2);
        assertThat(clientRepository.countTreatments(withoutTreatments.getId())).isZero();
        assertThat(offeringRepository.countTreatments(serviceId)).isEqualTo(2);
        assertThat(offeringRepository.countTreatments(unusedServiceId)).isZero();
    }
}
