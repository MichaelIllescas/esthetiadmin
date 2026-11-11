package com.esthetiadmin.backend.auth.service;

import com.esthetiadmin.backend.auth.entity.AppUser;
import com.esthetiadmin.backend.auth.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserInitializerTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AppUserRepository repository;
    private AdminUserInitializer initializer;

    @BeforeEach
    void setUp() {
        repository = mock(AppUserRepository.class);
        initializer = new AdminUserInitializer(repository, passwordEncoder, new AdminProperties("admin", "initial-password"));
    }

    @Test
    void createsTheUserWithAHashedPasswordWhenNoneExists() {
        when(repository.count()).thenReturn(0L);

        initializer.run(null);

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("admin");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("initial-password");
        assertThat(passwordEncoder.matches("initial-password", saved.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void doesNothingWhenAUserAlreadyExists() {
        when(repository.count()).thenReturn(1L);

        initializer.run(null);

        verify(repository, never()).save(any());
    }
}
