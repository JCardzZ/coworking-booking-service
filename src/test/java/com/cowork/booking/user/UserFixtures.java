package com.cowork.booking.user;

import com.cowork.booking.user.model.Permission;
import com.cowork.booking.user.model.Role;
import com.cowork.booking.user.model.User;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Builds domain objects whose ids and catalog fields are normally set by the database. */
public final class UserFixtures {

    private UserFixtures() {
    }

    public static Permission permission(String code) {
        Permission permission = BeanUtils.instantiateClass(Permission.class);
        ReflectionTestUtils.setField(permission, "code", code);
        ReflectionTestUtils.setField(permission, "description", code);
        return permission;
    }

    public static Role role(String name, String... permissionCodes) {
        return new Role(name, name, Arrays.stream(permissionCodes).map(UserFixtures::permission).collect(Collectors.toSet()));
    }

    public static User user(Long id, String email, Role role) {
        User user = new User(email, "hash", email, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
