package com.cowork.booking.user.repository;

import com.cowork.booking.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Role is fetched in the same query: no extra select per user (N+1)
    @EntityGraph(attributePaths = "role")
    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "role")
    Optional<User> findWithRoleById(Long id);

    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<User> findWithPermissionsById(Long id);

    @Override
    @EntityGraph(attributePaths = "role")
    Page<User> findAll(Pageable pageable);

    boolean existsByEmailIgnoreCase(String email);
}
