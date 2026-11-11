package com.esthetiadmin.backend.client.controller;

import com.esthetiadmin.backend.client.dto.BirthdayResponse;
import com.esthetiadmin.backend.client.dto.ClientRequest;
import com.esthetiadmin.backend.client.dto.ClientResponse;
import com.esthetiadmin.backend.client.dto.ExistsResponse;
import com.esthetiadmin.backend.client.service.ClientService;
import com.esthetiadmin.backend.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController implements ClientControllerDocs {

    private final ClientService clientService;

    @Override
    @GetMapping
    public PageResponse<ClientResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return clientService.search(search, page, size);
    }

    @Override
    @GetMapping("/exists")
    public ExistsResponse exists(@RequestParam String firstName, @RequestParam String lastName,
                                 @RequestParam(required = false) Long excludeId) {
        return new ExistsResponse(clientService.existsWithName(firstName, lastName, excludeId));
    }

    @Override
    @GetMapping("/birthdays")
    public List<BirthdayResponse> birthdays(
            @RequestParam(defaultValue = "30") int days) {
        return clientService.upcomingBirthdays(days);
    }

    @Override
    @GetMapping("/{id}")
    public ClientResponse get(@PathVariable Long id) {
        return clientService.get(id);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@RequestBody ClientRequest request) {
        return clientService.create(request);
    }

    @Override
    @PutMapping("/{id}")
    public ClientResponse update(@PathVariable Long id, @RequestBody ClientRequest request) {
        return clientService.update(id, request);
    }

    @Override
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        clientService.delete(id);
    }
}
