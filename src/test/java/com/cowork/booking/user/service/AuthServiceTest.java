package com.cowork.booking.user.service;

import com.cowork.booking.common.AppConstants;
import com.cowork.booking.common.AuthenticationFailedException;
import com.cowork.booking.common.BusinessRuleException;
import com.cowork.booking.user.dto.LoginRequest;
import com.cowork.booking.user.dto.RegisterRequest;
import com.cowork.booking.user.dto.TokenResponse;
import com.cowork.booking.user.dto.UserResponse;
import com.cowork.booking.user.mapper.UserMapper;
import com.cowork.booking.user.model.UserStatus;
import com.cowork.booking.user.model.User;
import com.cowork.booking.user.repository.RoleRepository;
import com.cowork.booking.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static com.cowork.booking.user.UserFixtures.role;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // Low cost keeps the tests fast; the algorithm is the same as in production.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private TokenService tokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, tokenService, new UserMapper());
    }

    @Test
    void registerCreatesUserWithRoleUserAndHashedPassword() {
        when(userRepository.existsByEmailIgnoreCase("ana@coworking.com")).thenReturn(false);
        when(roleRepository.findByNameIgnoreCase("USER")).thenReturn(Optional.of(role("USER")));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(new RegisterRequest("  Ana@Coworking.com ", "Secreta123", " Ana Pérez "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("ana@coworking.com");
        assertThat(saved.getValue().getRole().getName()).isEqualTo("USER");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("Secreta123");
        assertThat(passwordEncoder.matches("Secreta123", saved.getValue().getPasswordHash())).isTrue();
        assertThat(response.fullName()).isEqualTo("Ana Pérez");
    }

    @Test
    void registerRejectsEmailAlreadyInUse() {
        when(userRepository.existsByEmailIgnoreCase("ana@coworking.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("ana@coworking.com", "Secreta123", "Ana")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code", "conflictingField")
                .containsExactly(AppConstants.ErrorCodes.EMAIL_TAKEN, "email");
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokenWhenCredentialsMatch() {
        User user = new User("ana@coworking.com", passwordEncoder.encode("Secreta123"), "Ana", role("USER"));
        TokenResponse token = new TokenResponse("jwt", "Bearer", 3600);
        when(userRepository.findByEmailIgnoreCase("ana@coworking.com")).thenReturn(Optional.of(user));
        when(tokenService.issue(user)).thenReturn(token);

        assertThat(authService.login(new LoginRequest("ANA@coworking.com", "Secreta123"))).isEqualTo(token);
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = new User("ana@coworking.com", passwordEncoder.encode("Secreta123"), "Ana", role("USER"));
        when(userRepository.findByEmailIgnoreCase("ana@coworking.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@coworking.com", "otra")))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(tokenService, never()).issue(any());
    }

    @Test
    void loginRejectsUnknownEmailWithSameError() {
        when(userRepository.findByEmailIgnoreCase("nadie@coworking.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nadie@coworking.com", "Secreta123")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage(AppConstants.Messages.Auth.INVALID_CREDENTIALS_DETAIL);
    }

    @Test
    void loginRejectsDisabledAccountOnlyAfterCorrectPassword() {
        User user = new User("ana@coworking.com", passwordEncoder.encode("Secreta123"), "Ana", role("USER"));
        user.changeStatus(UserStatus.DISABLED);
        when(userRepository.findByEmailIgnoreCase("ana@coworking.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@coworking.com", "Secreta123")))
                .isInstanceOf(AuthenticationFailedException.class)
                .extracting("code").isEqualTo(AppConstants.ErrorCodes.ACCOUNT_DISABLED);
        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@coworking.com", "otra")))
                .extracting("code").isEqualTo(AppConstants.ErrorCodes.INVALID_CREDENTIALS);
        verify(tokenService, never()).issue(any());
    }

    @Test
    void requestsNeverExposePasswordsInToString() {
        assertThat(new RegisterRequest("ana@coworking.com", "Secreta123", "Ana").toString()).doesNotContain("Secreta123");
        assertThat(new LoginRequest("ana@coworking.com", "Secreta123").toString()).doesNotContain("Secreta123");
    }
}
