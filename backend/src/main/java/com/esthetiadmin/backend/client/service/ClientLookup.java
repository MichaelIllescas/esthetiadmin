package com.esthetiadmin.backend.client.service;

import com.esthetiadmin.backend.client.dto.ClientContact;
import com.esthetiadmin.backend.client.entity.Client;
import com.esthetiadmin.backend.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The door of the client module for the rest of the modules, which must not reach its repository. It only reads,
 * except for {@link #fillMissingEmail}: an appointment's email is copied to a client who has none (ADR 016).
 */
@Service
@RequiredArgsConstructor
public class ClientLookup {

    private static final Logger log = LoggerFactory.getLogger(ClientLookup.class);

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return clientRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ClientContact> findContact(Long id) {
        return clientRepository.findById(id)
                .map(client -> new ClientContact(client.getId(), client.getFirstName(), client.getLastName(),
                        client.getPhone(), client.getEmail()));
    }

    /** Gives the client the email when she has none; one she already has is never overwritten. */
    @Transactional
    public void fillMissingEmail(Long id, String email) {
        clientRepository.findById(id).ifPresent(client -> {
            if (client.fillMissingEmail(email)) {
                log.info("Client email filled from an appointment: clientId={}", id);
            }
        });
    }

    /** Display names ("Diaz, Carina") by client id. */
    @Transactional(readOnly = true)
    public Map<Long, String> namesByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return clientRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Client::getId, client -> client.getLastName() + ", " + client.getFirstName()));
    }
}
