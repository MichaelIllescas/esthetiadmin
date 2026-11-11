package com.esthetiadmin.backend.client.service;

import com.esthetiadmin.backend.client.dto.BirthdayResponse;
import com.esthetiadmin.backend.client.dto.ClientRequest;
import com.esthetiadmin.backend.client.dto.ClientResponse;
import com.esthetiadmin.backend.client.entity.Client;
import com.esthetiadmin.backend.client.repository.ClientRepository;
import com.esthetiadmin.backend.common.exception.ConflictException;
import com.esthetiadmin.backend.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);

    private ClientRepository repository;
    private ClientService service;

    @BeforeEach
    void setUp() {
        repository = mock(ClientRepository.class);
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new ClientService(repository, clock);
    }

    @Test
    void createTrimsTheNamesAndStoresBlankOptionalFieldsAsNothing() {
        when(repository.save(any(Client.class))).thenAnswer(call -> call.getArgument(0));

        ClientResponse response = service.create(new ClientRequest("  Carina ", " Diaz ", "  ", null, "   ", null));

        assertThat(response.firstName()).isEqualTo("Carina");
        assertThat(response.lastName()).isEqualTo("Diaz");
        assertThat(response.phone()).isNull();
        assertThat(response.notes()).isNull();
        assertThat(response.age()).isNull();
    }

    @Test
    void theAgeIsCalculatedFromTheBirthDate() {
        when(repository.save(any(Client.class))).thenAnswer(call -> call.getArgument(0));

        // The birthday of 1985-11-03 has not happened yet on 2026-09-25, so the client is still 40.
        ClientResponse response = service.create(
                new ClientRequest("Carina", "Diaz", null, LocalDate.of(1985, 11, 3), null, null));

        assertThat(response.age()).isEqualTo(40);
    }

    @Test
    void theAgeCountsTheBirthdayOnTheSameDay() {
        when(repository.save(any(Client.class))).thenAnswer(call -> call.getArgument(0));

        ClientResponse response = service.create(
                new ClientRequest("Carina", "Diaz", null, LocalDate.of(1985, 9, 25), null, null));

        assertThat(response.age()).isEqualTo(41);
    }

    @Test
    void getFailsWhenTheClientDoesNotExist() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(9L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("El cliente no existe");
    }

    @Test
    void updateChangesTheData() {
        Client client = client(1L, "Carina", "Diaz", null);
        when(repository.findById(1L)).thenReturn(Optional.of(client));

        ClientResponse response = service.update(1L,
                new ClientRequest("Carina", "Díaz", "3415551234", null, "Piel sensible", null));

        assertThat(response.lastName()).isEqualTo("Díaz");
        assertThat(response.phone()).isEqualTo("3415551234");
        assertThat(response.notes()).isEqualTo("Piel sensible");
    }

    @Test
    void updateFailsWhenTheClientDoesNotExist() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(9L, new ClientRequest("A", "B", null, null, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRemovesAClientWithoutTreatments() {
        Client client = client(1L, "Carina", "Diaz", null);
        when(repository.findById(1L)).thenReturn(Optional.of(client));
        when(repository.countTreatments(1L)).thenReturn(0L);

        service.delete(1L);

        verify(repository).delete(client);
    }

    @Test
    void deleteRejectsAClientWithTreatments() {
        when(repository.findById(1L)).thenReturn(Optional.of(client(1L, "Carina", "Diaz", null)));
        when(repository.countTreatments(1L)).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("tratamientos registrados");
        verify(repository, never()).delete(any(Client.class));
    }

    @Test
    void existsWithNameTrimsAndCanIgnoreTheClientBeingEdited() {
        when(repository.existsByFirstNameAndLastName("Carina", "Diaz")).thenReturn(true);
        when(repository.existsByFirstNameAndLastNameAndIdNot("Carina", "Diaz", 5L)).thenReturn(false);

        assertThat(service.existsWithName(" Carina ", "Diaz ", null)).isTrue();
        assertThat(service.existsWithName("Carina", "Diaz", 5L)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchReturnsAPageOfClientsWithTheirAge() {
        Client client = client(1L, "Carina", "Diaz", LocalDate.of(1985, 11, 3));
        Page<Client> page = new PageImpl<>(List.of(client), org.springframework.data.domain.PageRequest.of(0, 20), 1);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        var result = service.search("carina", 0, 20);

        assertThat(result.content()).extracting(ClientResponse::age).containsExactly(40);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
    }

    @Test
    void birthdaysListsOnlyTheOnesInsideTheWindowSortedByProximity() {
        when(repository.findAllByBirthDateIsNotNull()).thenReturn(List.of(
                client(1L, "Later", "Zeta", LocalDate.of(1990, 10, 20)),   // 25 days
                client(2L, "Today", "Alfa", LocalDate.of(1980, 9, 25)),    // 0 days
                client(3L, "Soon", "Beta", LocalDate.of(2000, 9, 28)),     // 3 days
                client(4L, "Far", "Gamma", LocalDate.of(1975, 3, 1)),      // outside the window
                client(5L, "Passed", "Delta", LocalDate.of(1970, 9, 24))   // next year, outside the window
        ));

        List<BirthdayResponse> result = service.upcomingBirthdays(30);

        assertThat(result).extracting(BirthdayResponse::firstName).containsExactly("Today", "Soon", "Later");
        assertThat(result.get(0).daysUntil()).isZero();
        assertThat(result.get(0).turningAge()).isEqualTo(46);
        assertThat(result.get(1).nextBirthday()).isEqualTo(LocalDate.of(2026, 9, 28));
        assertThat(result.get(2).daysUntil()).isEqualTo(25);
    }

    @Test
    void birthdaysCrossTheEndOfTheYear() {
        Clock december = Clock.fixed(Instant.parse("2026-12-20T12:00:00Z"), ZoneOffset.UTC);
        ClientService decemberService = new ClientService(repository, december);
        when(repository.findAllByBirthDateIsNotNull())
                .thenReturn(List.of(client(1L, "New", "Year", LocalDate.of(1990, 1, 5))));

        List<BirthdayResponse> result = decemberService.upcomingBirthdays(30);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nextBirthday()).isEqualTo(LocalDate.of(2027, 1, 5));
        assertThat(result.get(0).daysUntil()).isEqualTo(16);
        assertThat(result.get(0).turningAge()).isEqualTo(37);
    }

    @Test
    void someoneBornOnFebruary29CelebratesOnFebruary28InANonLeapYear() {
        Clock february = Clock.fixed(Instant.parse("2027-02-20T12:00:00Z"), ZoneOffset.UTC);
        ClientService februaryService = new ClientService(repository, february);
        when(repository.findAllByBirthDateIsNotNull())
                .thenReturn(List.of(client(1L, "Leap", "Baby", LocalDate.of(2000, 2, 29))));

        List<BirthdayResponse> result = februaryService.upcomingBirthdays(30);

        assertThat(result.get(0).nextBirthday()).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(result.get(0).turningAge()).isEqualTo(27);
    }

    @Test
    void someoneBornOnFebruary29CelebratesOnFebruary29InALeapYear() {
        Clock february = Clock.fixed(Instant.parse("2028-02-20T12:00:00Z"), ZoneOffset.UTC);
        ClientService februaryService = new ClientService(repository, february);
        when(repository.findAllByBirthDateIsNotNull())
                .thenReturn(List.of(client(1L, "Leap", "Baby", LocalDate.of(2000, 2, 29))));

        assertThat(februaryService.upcomingBirthdays(30).get(0).nextBirthday()).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    private Client client(Long id, String firstName, String lastName, LocalDate birthDate) {
        Client client = new Client(firstName, lastName, null, birthDate, null);
        ReflectionTestUtils.setField(client, "id", id);
        return client;
    }
}
