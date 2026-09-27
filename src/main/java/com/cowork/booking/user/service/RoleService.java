package com.cowork.booking.user.service;

import com.cowork.booking.common.AppConstants.Audit;
import com.cowork.booking.common.AppConstants.Caches;
import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Messages;
import com.cowork.booking.common.AppConstants.Permissions;
import com.cowork.booking.common.AppConstants.Security;
import com.cowork.booking.common.Audited;
import com.cowork.booking.common.BusinessRuleException;
import com.cowork.booking.common.ResourceNotFoundException;
import com.cowork.booking.common.UnprocessableOperationException;
import com.cowork.booking.user.dto.CreateRoleRequest;
import com.cowork.booking.user.dto.PermissionResponse;
import com.cowork.booking.user.dto.RoleResponse;
import com.cowork.booking.user.dto.UpdateRolePermissionsRequest;
import com.cowork.booking.user.mapper.RoleMapper;
import com.cowork.booking.user.model.Permission;
import com.cowork.booking.user.model.Role;
import com.cowork.booking.user.repository.PermissionRepository;
import com.cowork.booking.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    public List<PermissionResponse> findAllPermissions() {
        return permissionRepository.findAllByOrderByCodeAsc().stream().map(roleMapper::toResponse).toList();
    }

    public List<RoleResponse> findAll() {
        return roleRepository.findAllByOrderByNameAsc().stream().map(roleMapper::toResponse).toList();
    }

    @Transactional
    @Audited(Audit.ROLE_CREATE)
    public RoleResponse create(CreateRoleRequest request) {
        String name = request.name().trim().toUpperCase(Locale.ROOT);
        if (roleRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException(ErrorCodes.ROLE_NAME_TAKEN, Messages.Role.NAME_TAKEN.formatted(name),
                    Messages.Role.NAME_FIELD);
        }
        Role role = new Role(name, request.description().trim(), resolve(request.permissions()));
        return roleMapper.toResponse(roleRepository.save(role));
    }

    // Affects every user with this role, so clear the whole cache
    @Transactional
    @Audited(Audit.ROLE_PERMISSIONS_UPDATE)
    @CacheEvict(cacheNames = Caches.USER_AUTHORIZATION, allEntries = true)
    public RoleResponse updatePermissions(Long roleId, UpdateRolePermissionsRequest request) {
        Role role = roleRepository.findWithPermissionsById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(Messages.Role.RESOURCE_TYPE, roleId,
                        Messages.Role.NOT_FOUND.formatted(roleId)));
        Set<Permission> permissions = resolve(request.permissions());
        if (Security.ADMIN_ROLE.equals(role.getName())) {
            ensureAdminKeepsLockedPermissions(permissions);
        }
        role.replacePermissions(permissions);
        return roleMapper.toResponse(roleRepository.saveAndFlush(role));
    }

    private Set<Permission> resolve(Set<String> codes) {
        Set<String> normalized = codes.stream().map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<Permission> found = permissionRepository.findByCodeIn(normalized);
        if (found.size() != normalized.size()) {
            Set<String> unknown = new TreeSet<>(normalized);
            found.forEach(permission -> unknown.remove(permission.getCode()));
            throw new UnprocessableOperationException(ErrorCodes.UNKNOWN_PERMISSION,
                    Messages.Role.UNKNOWN_PERMISSIONS.formatted(unknown));
        }
        return new HashSet<>(found);
    }

    private static void ensureAdminKeepsLockedPermissions(Set<Permission> permissions) {
        Set<String> codes = permissions.stream().map(Permission::getCode).collect(Collectors.toSet());
        if (!codes.containsAll(Permissions.ADMIN_LOCKED)) {
            throw new UnprocessableOperationException(ErrorCodes.ADMIN_ROLE_LOCKOUT,
                    Messages.Role.ADMIN_LOCKOUT.formatted(new TreeSet<>(Permissions.ADMIN_LOCKED)));
        }
    }
}
