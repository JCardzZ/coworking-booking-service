package com.cowork.booking.space.repository;

import com.cowork.booking.space.dto.SpaceFilter;
import com.cowork.booking.space.model.Space;
import com.cowork.booking.space.model.SpaceType;
import com.cowork.booking.space.model.Space_;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SpaceSpecifications {

    private static final String LIKE_WILDCARD = "%";

    private SpaceSpecifications() {
    }

    /** Active spaces matching every filter that is present; absent filters are ignored. */
    public static Specification<Space> matching(SpaceFilter filter) {
        List<Specification<Space>> specs = new ArrayList<>();
        specs.add(isActive());
        if (filter.type() != null) {
            specs.add(hasType(filter.type()));
        }
        if (filter.minCapacity() != null) {
            specs.add(capacityAtLeast(filter.minCapacity()));
        }
        if (filter.location() != null && !filter.location().isBlank()) {
            specs.add(locationContains(filter.location()));
        }
        return Specification.allOf(specs);
    }

    static Specification<Space> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get(Space_.active));
    }

    static Specification<Space> hasType(SpaceType type) {
        return (root, query, cb) -> cb.equal(root.get(Space_.type), type);
    }

    static Specification<Space> capacityAtLeast(int minCapacity) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(Space_.capacity), minCapacity);
    }

    static Specification<Space> locationContains(String text) {
        return (root, query, cb) -> cb.like(cb.lower(root.get(Space_.location)), containsPattern(text));
    }

    private static String containsPattern(String text) {
        return LIKE_WILDCARD + text.trim().toLowerCase(Locale.ROOT) + LIKE_WILDCARD;
    }
}
