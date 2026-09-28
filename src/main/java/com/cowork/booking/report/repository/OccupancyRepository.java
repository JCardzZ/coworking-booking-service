package com.cowork.booking.report.repository;

import com.cowork.booking.space.model.Space;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OccupancyRepository extends Repository<Space, Long> {

    // FILTER: without it LEAST/GREATEST skip the nulls and an empty space shows 100%
    @Query(nativeQuery = true, value = """
            SELECT s.id AS spaceId,
                   s.name AS spaceName,
                   COUNT(r.id) AS confirmedReservations,
                   COALESCE(SUM(EXTRACT(EPOCH FROM LEAST(r.end_at, :to) - GREATEST(r.start_at, :from)))
                            FILTER (WHERE r.id IS NOT NULL), 0) AS reservedSeconds
            FROM spaces s
                     LEFT JOIN reservations r
                               ON r.space_id = s.id
                                   AND r.status = 'CONFIRMED'
                                   AND r.start_at < :to
                                   AND r.end_at > :from
            WHERE s.active
              AND (CAST(:spaceId AS BIGINT) IS NULL OR s.id = :spaceId)
            GROUP BY s.id, s.name
            ORDER BY s.name""")
    List<OccupancyRow> occupancy(@Param("spaceId") Long spaceId, @Param("from") Instant from, @Param("to") Instant to);
}
