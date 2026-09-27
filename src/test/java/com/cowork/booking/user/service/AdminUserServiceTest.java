package com.cowork.booking.user.service;

import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.UnprocessableOperationException;
import com.cowork.booking.user.dto.AdminCreateUserRequest;
import com.cowork.booking.user.dto.UpdateUserStatusRequest;
import com.cowork.booking.user.dto.UserResponse;
import com.cowork.booking.user.mapper.UserMapper;
import com.cowork.booking.user.model.User;
import com.cowork.booking.user.model.UserStatus;
import com.cowork.booking.user.repository.RoleRepository;
import com.cowork.booking.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static com.cowork.booking.user.UserFixtures.role;
import static com.cowork.booking.user.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(userRepository, roleRepository, new BCryptPasswordEncoder(4), new UserMapper());
        authenticateAs(1L);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAssignsTheRequestedRole() {
        when(userRepository.existsByEmailIgnoreCase("laura@coworking.com")).thenReturn(false);
        when(roleRepository.findByNameIgnoreCase("ADMIN")).thenReturn(Optional.of(role("ADMIN")));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = adminUserService.create(
                new AdminCreateUserRequest("Laura@Coworking.com", "Secreta123", "Laura", "admin"));

        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.email()).isEqualTo("laura@coworking.com");
    }

    @Test
    void createRejectsUnknownRole() {
        when(userRepository.existsByEmailIgnoreCase("laura@coworking.com")).thenReturn(false);
        when(roleRepository.findByNameIgnoreCase("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.create(
                new AdminCreateUserRequest("laura@coworking.com", "Secreta123", "Laura", "ghost")))
                .isInstanceOf(UnprocessableOperationException.class)
                .extracting("code").isEqualTo(ErrorCodes.UNKNOWN_ROLE);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateStatusDisablesAnotherUser() {
        User ana = user(2L, "ana@coworking.com", role("USER"));
        when(userRepository.findWithRoleById(2L)).thenReturn(Optional.of(ana));
        when(userRepository.saveAndFlush(ana)).thenReturn(ana);

        UserResponse response = adminUserService.updateStatus(2L, new UpdateUserStatusRequest(UserStatus.DISABLED));

        assertThat(response.status()).isEqualTo(UserStatus.DISABLED);
        assertThat(ana.isActive()).isFalse();
    }

    @Test
    void adminCannotChangeOwnStatus() {
        assertThatThrownBy(() -> adminUserService.updateStatus(1L, new UpdateUserStatusRequest(UserStatus.DISABLED)))
                .isInstanceOf(UnprocessableOperationException.class)
                .extracting("code").isEqualTo(ErrorCodes.SELF_STATUS_CHANGE);
        verify(userRepository, never()).findWithRoleById(any());
    }

    private static void authenticateAs(Long userId) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(String.valueOf(userId)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }
}
