package com.cowork.booking.user.mapper;

import com.cowork.booking.user.dto.PermissionResponse;
import com.cowork.booking.user.dto.RoleResponse;
import com.cowork.booking.user.model.Permission;
import com.cowork.booking.user.model.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), role.isSystem(),
                role.permissionCodes().stream().sorted().toList());
    }

    public PermissionResponse toResponse(Permission permission) {
        return new PermissionResponse(permission.getCode(), permission.getDescription());
    }
}
