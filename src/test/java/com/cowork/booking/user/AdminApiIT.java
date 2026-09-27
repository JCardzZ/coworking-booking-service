package com.cowork.booking.user;

import com.cowork.booking.common.AppConstants.Api;
import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminApiIT extends IntegrationTest {

    private static final String ROLES = Api.BASE_PATH + Api.ADMIN_ROLES;
    private static final String USERS = Api.BASE_PATH + Api.ADMIN_USERS;
    private static final String PASSWORD = "Staff1234!";

    @Test
    void rolesArePermissionSetsThatCanChangeAtRuntime() throws Exception {
        perform(get(Api.BASE_PATH + Api.ADMIN_PERMISSIONS), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("SPACE_READ")));
        perform(get(ROLES), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("ADMIN")));

        String roleName = "staff_" + System.nanoTime();
        Long roleId = idOf(createRole(roleName, "SPACE_READ").andExpect(status().isCreated()));
        createRole(roleName, "SPACE_READ")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodes.ROLE_NAME_TAKEN));
        createRole("other_" + System.nanoTime(), "NOPE")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNKNOWN_PERMISSION));

        String email = "staff-" + System.nanoTime() + "@test.com";
        createUser(email, roleName).andExpect(status().isCreated());
        String staffToken = login(email, PASSWORD);

        // same token, new permission: no need to log in again
        perform(post(Api.BASE_PATH + Api.SPACES).contentType(MediaType.APPLICATION_JSON).content(space()), staffToken)
                .andExpect(status().isForbidden());
        setPermissions(roleId, "SPACE_READ", "SPACE_WRITE").andExpect(status().isOk());
        perform(post(Api.BASE_PATH + Api.SPACES).contentType(MediaType.APPLICATION_JSON).content(space()), staffToken)
                .andExpect(status().isCreated());

        setPermissions(999999L, "SPACE_READ").andExpect(status().isNotFound());
    }

    @Test
    void adminRoleCantLoseItsAdministrationPermissions() throws Exception {
        String roles = perform(get(ROLES), adminToken).andReturn().getResponse().getContentAsString();
        Long adminRoleId = null;
        List<String> adminPermissions = new ArrayList<>();
        for (var role : objectMapper.readTree(roles)) {
            if ("ADMIN".equals(role.get("name").asText())) {
                adminRoleId = role.get("id").asLong();
                role.get("permissions").forEach(permission -> adminPermissions.add(permission.asText()));
            }
        }

        setPermissions(adminRoleId, "SPACE_READ")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCodes.ADMIN_ROLE_LOCKOUT));

        // saving it with everything it already had is fine
        setPermissions(adminRoleId, adminPermissions.toArray(String[]::new)).andExpect(status().isOk());
    }

    @Test
    void adminManagesUsersAndCanBlockThem() throws Exception {
        String email = "user-" + System.nanoTime() + "@test.com";
        Long userId = idOf(createUser(email, "USER").andExpect(status().isCreated()));
        String userToken = login(email, PASSWORD);

        createUser(email, "USER")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodes.EMAIL_TAKEN));
        createUser("x-" + email, "NO_EXISTE")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNKNOWN_ROLE));

        perform(get(USERS).param("sort", "email,asc"), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].email", hasItem(email)));
        perform(get(USERS + "/{id}", userId), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        perform(get(USERS + "/999999"), adminToken).andExpect(status().isNotFound());

        // a blocked user is out right away, even with a token that hasn't expired
        setStatus(userId, "DISABLED").andExpect(status().isOk());
        perform(get(Api.BASE_PATH + Api.USERS + Api.ME), userToken).andExpect(status().isUnauthorized());
        perform(post(Api.BASE_PATH + Api.AUTH + Api.LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, PASSWORD)), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.ACCOUNT_DISABLED));

        setStatus(userId, "ACTIVE").andExpect(status().isOk());
        perform(get(Api.BASE_PATH + Api.USERS + Api.ME), login(email, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void adminCantBlockThemselves() throws Exception {
        Long adminId = idOf(perform(get(Api.BASE_PATH + Api.USERS + Api.ME), adminToken));

        setStatus(adminId, "DISABLED")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCodes.SELF_STATUS_CHANGE));
    }

    private ResultActions createRole(String name, String... permissions) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", name, "description", "Rol de prueba", "permissions", permissions));
        return perform(post(ROLES).contentType(MediaType.APPLICATION_JSON).content(body), adminToken);
    }

    private ResultActions setPermissions(Long roleId, String... permissions) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("permissions", permissions));
        return perform(put(ROLES + "/{id}" + Api.PERMISSIONS, roleId).contentType(MediaType.APPLICATION_JSON).content(body),
                adminToken);
    }

    private ResultActions createUser(String email, String role) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", PASSWORD, "fullName", "Persona de prueba", "role", role));
        return perform(post(USERS).contentType(MediaType.APPLICATION_JSON).content(body), adminToken);
    }

    private ResultActions setStatus(Long userId, String status) throws Exception {
        return perform(put(USERS + "/{id}" + Api.STATUS, userId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"%s\"}".formatted(status)), adminToken);
    }

    private static String space() {
        return """
                {"name": "Sala staff %d", "type": "DESK", "capacity": 1, "location": "Piso 1", "hourlyRate": 10}
                """.formatted(System.nanoTime());
    }
}
