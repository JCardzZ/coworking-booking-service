package com.cowork.booking.space.model;

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

import java.math.BigDecimal;

import static com.cowork.booking.common.AppConstants.Limits.ENUM_MAX;
import static com.cowork.booking.common.AppConstants.Limits.MONEY_FRACTION_DIGITS;
import static com.cowork.booking.common.AppConstants.Limits.MONEY_INTEGER_DIGITS;
import static com.cowork.booking.common.AppConstants.Limits.SPACE_LOCATION_MAX;
import static com.cowork.booking.common.AppConstants.Limits.SPACE_NAME_MAX;

@Entity
@Table(name = "spaces")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Space extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = SPACE_NAME_MAX)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = ENUM_MAX)
    private SpaceType type;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false, length = SPACE_LOCATION_MAX)
    private String location;

    @Column(name = "hourly_rate", nullable = false, precision = MONEY_INTEGER_DIGITS + MONEY_FRACTION_DIGITS, scale = MONEY_FRACTION_DIGITS)
    private BigDecimal hourlyRate;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private long version;

    public Space(String name, SpaceType type, int capacity, String location, BigDecimal hourlyRate) {
        update(name, type, capacity, location, hourlyRate);
    }

    public void update(String name, SpaceType type, int capacity, String location, BigDecimal hourlyRate) {
        this.name = name;
        this.type = type;
        this.capacity = capacity;
        this.location = location;
        this.hourlyRate = hourlyRate;
    }

    // Soft delete: kept for existing reservations and reports.
    public void deactivate() {
        this.active = false;
    }
}
