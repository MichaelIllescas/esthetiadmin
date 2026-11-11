package com.esthetiadmin.backend.client.service;

import com.esthetiadmin.backend.client.dto.BirthdayResponse;
import com.esthetiadmin.backend.client.dto.ClientRequest;
import com.esthetiadmin.backend.common.contact.ContactData;
import com.esthetiadmin.backend.client.dto.ClientResponse;
import com.esthetiadmin.backend.client.entity.Client;
import com.esthetiadmin.backend.client.repository.ClientRepository;
import com.esthetiadmin.backend.client.repository.ClientSpecifications;
import com.esthetiadmin.backend.common.dto.PageResponse;
import com.esthetiadmin.backend.common.exception.ConflictException;
import com.esthetiadmin.backend.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private static final Sort NAME_ORDER = Sort.by("lastName", "firstName", "id");

    private final ClientRepository clientRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> search(String search, int page, int size) {
        LocalDate today = today();
        return PageResponse.from(
                clientRepository.findAll(ClientSpecifications.matching(search), PageRequest.of(page, size, NAME_ORDER)),
                client -> ClientResponse.from(client, today));
    }

    @Transactional(readOnly = true)
    public ClientResponse get(Long id) {
        return ClientResponse.from(getClient(id), today());
    }

    @Transactional
    public ClientResponse create(ClientRequest request) {
        Client client = new Client(request.firstName().trim(), request.lastName().trim(),
                blankToNull(request.phone()), request.birthDate(), blankToNull(request.notes()));
        client.changeEmail(ContactData.email(request.email()));
        Client saved = clientRepository.save(client);
        return ClientResponse.from(saved, today());
    }

    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = getClient(id);
        client.update(request.firstName().trim(), request.lastName().trim(), blankToNull(request.phone()),
                request.birthDate(), blankToNull(request.notes()));
        client.changeEmail(ContactData.email(request.email()));
        return ClientResponse.from(client, today());
    }

    @Transactional
    public void delete(Long id) {
        Client client = getClient(id);
        if (clientRepository.countTreatments(id) > 0) {
            throw new ConflictException(
                    "El cliente tiene tratamientos registrados. Eliminá esos tratamientos primero");
        }
        if (clientRepository.countAppointments(id) > 0) {
            throw new ConflictException("El cliente tiene turnos registrados, así que no se puede eliminar");
        }
        clientRepository.delete(client);
    }

    /** Whether another client already has this first and last name, so the screen can warn before saving. */
    @Transactional(readOnly = true)
    public boolean existsWithName(String firstName, String lastName, Long excludeId) {
        String first = firstName.trim();
        String last = lastName.trim();
        return excludeId == null
                ? clientRepository.existsByFirstNameAndLastName(first, last)
                : clientRepository.existsByFirstNameAndLastNameAndIdNot(first, last, excludeId);
    }

    @Transactional(readOnly = true)
    public List<BirthdayResponse> upcomingBirthdays(int days) {
        LocalDate today = today();
        return clientRepository.findAllByBirthDateIsNotNull().stream()
                .map(client -> toBirthday(client, today))
                .filter(birthday -> birthday.daysUntil() <= days)
                .sorted(Comparator.comparingLong(BirthdayResponse::daysUntil)
                        .thenComparing(BirthdayResponse::lastName)
                        .thenComparing(BirthdayResponse::firstName))
                .toList();
    }

    private BirthdayResponse toBirthday(Client client, LocalDate today) {
        LocalDate birthDate = client.getBirthDate();
        LocalDate next = occurrenceIn(birthDate, today.getYear());
        if (next.isBefore(today)) {
            next = occurrenceIn(birthDate, today.getYear() + 1);
        }
        return new BirthdayResponse(client.getId(), client.getFirstName(), client.getLastName(), birthDate, next,
                next.getYear() - birthDate.getYear(), ChronoUnit.DAYS.between(today, next));
    }

    /** Someone born on February 29 celebrates on February 28 in the years that are not leap years. */
    private LocalDate occurrenceIn(LocalDate birthDate, int year) {
        if (birthDate.getMonth() == Month.FEBRUARY && birthDate.getDayOfMonth() == 29 && !Year.isLeap(year)) {
            return LocalDate.of(year, Month.FEBRUARY, 28);
        }
        return LocalDate.of(year, birthDate.getMonth(), birthDate.getDayOfMonth());
    }

    private Client getClient(Long id) {
        return clientRepository.findById(id).orElseThrow(() -> new NotFoundException("El cliente no existe"));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
