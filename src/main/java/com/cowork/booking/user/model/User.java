package com.cowork.booking.user.model;

import com.cowork.booking.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.cowork.booking.common.AppConstants.Limits.EMAIL_MAX;
import static com.cowork.booking.common.AppConstants.Limits.ENUM_MAX;
import static com.cowork.booking.common.AppConstants.Limits.FULL_NAME_MAX;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = EMAIL_MAX)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = FULL_NAME_MAX)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = ENUM_MAX)
    private Role role;

    @Version
    private long version;

    public User(String email, String passwordHash, String fullName, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }
}
