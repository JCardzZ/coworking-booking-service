package com.cowork.booking.user.repository;

import com.cowork.booking.user.model.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    // Permissions fetched in the same query: no extra select per role (N+1)
    @EntityGraph(attributePaths = "permissions")
    List<Role> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsById(Long id);
}
