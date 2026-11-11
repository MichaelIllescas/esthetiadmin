package com.esthetiadmin.backend.auth.service;

import com.esthetiadmin.backend.auth.entity.AppUser;
import com.esthetiadmin.backend.auth.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the only user of the system on the first startup, from environment variables. */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminProperties.class)
public class AdminUserInitializer implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (appUserRepository.count() > 0) {
            return;
        }
        appUserRepository.save(new AppUser(properties.username(), passwordEncoder.encode(properties.password())));
        log.info("Initial user '{}' created", properties.username());
    }
}
