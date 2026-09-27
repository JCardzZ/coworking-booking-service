package com.cowork.booking.user.service;

import com.cowork.booking.config.SecurityProperties;
import com.cowork.booking.user.repository.RoleRepository;
import com.cowork.booking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void doesNothingWhenTheAdminAlreadyExists() {
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.Jwt("test-only-secret-0123456789abcdef0123", Duration.ofHours(1)),
                new SecurityProperties.Admin("Admin@Test.com", "Admin123!", "Administrador"));
        when(userRepository.existsByEmailIgnoreCase("admin@test.com")).thenReturn(true);

        new AdminInitializer(userRepository, roleRepository, passwordEncoder, properties).run(new DefaultApplicationArguments());

        verify(userRepository, never()).save(any());
    }
}
