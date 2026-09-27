package com.cowork.booking.user.repository;

import com.cowork.booking.user.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    List<Permission> findByCodeIn(Collection<String> codes);

    List<Permission> findAllByOrderByCodeAsc();
}
