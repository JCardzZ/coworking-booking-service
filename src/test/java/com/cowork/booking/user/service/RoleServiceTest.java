package com.cowork.booking.user.service;

import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.BusinessRuleException;
import com.cowork.booking.common.UnprocessableOperationException;
import com.cowork.booking.user.dto.CreateRoleRequest;
import com.cowork.booking.user.dto.RoleResponse;
import com.cowork.booking.user.dto.UpdateRolePermissionsRequest;
import com.cowork.booking.user.mapper.RoleMapper;
import com.cowork.booking.user.model.Role;
import com.cowork.booking.user.repository.PermissionRepository;
import com.cowork.booking.user.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.cowork.booking.user.UserFixtures.permission;
import static com.cowork.booking.user.UserFixtures.role;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PermissionRepository permissionRepository;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService(roleRepository, permissionRepository, new RoleMapper());
    }

    @Test
    void createNormalizesNameAndAssignsPermissions() {
        when(roleRepository.existsByNameIgnoreCase("RECEPTION")).thenReturn(false);
        when(permissionRepository.findByCodeIn(anyCollection()))
                .thenReturn(List.of(permission("SPACE_READ"), permission("RESERVATION_READ_ALL")));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleResponse response = roleService.create(
                new CreateRoleRequest(" reception ", "Recepción", Set.of("space_read", "RESERVATION_READ_ALL")));

        assertThat(response.name()).isEqualTo("RECEPTION");
        assertThat(response.permissions()).containsExactly("RESERVATION_READ_ALL", "SPACE_READ");
        assertThat(response.system()).isFalse();
    }

    @Test
    void createRejectsDuplicateName() {
        when(roleRepository.existsByNameIgnoreCase("ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> roleService.create(new CreateRoleRequest("ADMIN", "x", Set.of("SPACE_READ"))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo(ErrorCodes.ROLE_NAME_TAKEN);
        verify(roleRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownPermissions() {
        when(roleRepository.existsByNameIgnoreCase("RECEPTION")).thenReturn(false);
        when(permissionRepository.findByCodeIn(anyCollection())).thenReturn(List.of(permission("SPACE_READ")));

        assertThatThrownBy(() -> roleService.create(
                new CreateRoleRequest("RECEPTION", "x", Set.of("SPACE_READ", "FLY_TO_MOON"))))
                .isInstanceOf(UnprocessableOperationException.class)
                .hasMessageContaining("FLY_TO_MOON")
                .extracting("code").isEqualTo(ErrorCodes.UNKNOWN_PERMISSION);
    }

    @Test
    void adminRoleCannotLoseAdministrationPermissions() {
        Role admin = role("ADMIN", "USER_MANAGE", "RBAC_MANAGE", "SPACE_WRITE");
        when(roleRepository.findWithPermissionsById(1L)).thenReturn(Optional.of(admin));
        when(permissionRepository.findByCodeIn(anyCollection())).thenReturn(List.of(permission("SPACE_WRITE")));

        assertThatThrownBy(() -> roleService.updatePermissions(1L, new UpdateRolePermissionsRequest(Set.of("SPACE_WRITE"))))
                .isInstanceOf(UnprocessableOperationException.class)
                .extracting("code").isEqualTo(ErrorCodes.ADMIN_ROLE_LOCKOUT);
        assertThat(admin.permissionCodes()).contains("USER_MANAGE", "RBAC_MANAGE");
    }

    @Test
    void updatePermissionsReplacesTheWholeSet() {
        Role reception = role("RECEPTION", "SPACE_READ");
        when(roleRepository.findWithPermissionsById(3L)).thenReturn(Optional.of(reception));
        when(permissionRepository.findByCodeIn(anyCollection())).thenReturn(List.of(permission("RESERVATION_READ_ALL")));
        when(roleRepository.saveAndFlush(reception)).thenReturn(reception);

        RoleResponse response = roleService.updatePermissions(3L, new UpdateRolePermissionsRequest(Set.of("RESERVATION_READ_ALL")));

        assertThat(response.permissions()).containsExactly("RESERVATION_READ_ALL");
    }
}
