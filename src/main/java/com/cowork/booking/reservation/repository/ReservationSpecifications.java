package com.cowork.booking.reservation.repository;

import com.cowork.booking.reservation.dto.ReservationFilter;
import com.cowork.booking.reservation.model.Reservation;
import com.cowork.booking.reservation.model.Reservation_;
import com.cowork.booking.space.model.Space_;
import com.cowork.booking.user.model.User_;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ReservationSpecifications {

    private ReservationSpecifications() {
    }

    // ownerId is null for admins, so they see everyone's reservations
    public static Specification<Reservation> matching(ReservationFilter filter, Long ownerId) {
        List<Specification<Reservation>> specs = new ArrayList<>();
        if (ownerId != null) {
            specs.add((root, query, cb) -> cb.equal(root.get(Reservation_.user).get(User_.id), ownerId));
        }
        if (filter.spaceId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get(Reservation_.space).get(Space_.id), filter.spaceId()));
        }
        if (filter.status() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get(Reservation_.status), filter.status()));
        }
        if (filter.from() != null) {
            specs.add((root, query, cb) -> cb.greaterThan(root.get(Reservation_.endAt), filter.from()));
        }
        if (filter.to() != null) {
            specs.add((root, query, cb) -> cb.lessThan(root.get(Reservation_.startAt), filter.to()));
        }
        return Specification.allOf(specs);
    }
}
