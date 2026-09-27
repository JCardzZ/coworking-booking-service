package com.cowork.booking.reservation.repository;

import com.cowork.booking.reservation.model.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    // fetch space and user in one query, avoids N+1 on listings
    @Override
    @EntityGraph(attributePaths = {"space", "user"})
    Page<Reservation> findAll(Specification<Reservation> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"space", "user"})
    Optional<Reservation> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"space", "user"})
    Optional<Reservation> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    // end is exclusive: a 9-11 booking and an 11-12 one don't clash
    @Query("""
            select count(r) > 0 from Reservation r
            where r.space.id = :spaceId
              and r.status <> com.cowork.booking.reservation.model.ReservationStatus.CANCELLED
              and r.startAt < :endAt and r.endAt > :startAt""")
    boolean existsOverlapping(@Param("spaceId") Long spaceId, @Param("startAt") Instant startAt,
                              @Param("endAt") Instant endAt);
}
