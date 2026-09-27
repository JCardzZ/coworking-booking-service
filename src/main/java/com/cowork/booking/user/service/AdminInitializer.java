package com.cowork.booking.user.service;

import com.cowork.booking.common.AppConstants.Messages;
import com.cowork.booking.common.AppConstants.Security;
import com.cowork.booking.config.SecurityProperties;
import com.cowork.booking.user.model.Role;
import com.cowork.booking.user.model.User;
import com.cowork.booking.user.repository.RoleRepository;
import com.cowork.booking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the initial ADMIN from configuration; public registration only creates USERs. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SecurityProperties.Admin admin = securityProperties.admin();
        String email = AuthService.normalize(admin.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        Role adminRole = roleRepository.findByNameIgnoreCase(Security.ADMIN_ROLE).orElseThrow();
        userRepository.save(new User(email, passwordEncoder.encode(admin.password()), admin.fullName(), adminRole));
        log.info(Messages.Auth.LOG_ADMIN_CREATED, email);
    }
}
