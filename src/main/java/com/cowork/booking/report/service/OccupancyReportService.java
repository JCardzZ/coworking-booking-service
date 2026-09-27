package com.cowork.booking.report.service;

import com.cowork.booking.common.AppConstants.Caches;
import com.cowork.booking.common.AppConstants.Messages;
import com.cowork.booking.common.ResourceNotFoundException;
import com.cowork.booking.report.dto.OccupancyReportRequest;
import com.cowork.booking.report.dto.OccupancyReportResponse;
import com.cowork.booking.report.dto.SpaceOccupancy;
import com.cowork.booking.report.repository.OccupancyRepository;
import com.cowork.booking.report.repository.OccupancyRow;
import com.cowork.booking.space.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OccupancyReportService {

    private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600);
    private static final BigDecimal HOURS_PER_DAY = BigDecimal.valueOf(24);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int SCALE = 2;

    private final OccupancyRepository occupancyRepository;
    private final SpaceRepository spaceRepository;

    // evicted when a reservation is confirmed or cancelled and when a space changes
    @Cacheable(Caches.OCCUPANCY_REPORT)
    public OccupancyReportResponse occupancy(OccupancyReportRequest request) {
        if (request.spaceId() != null && spaceRepository.findByIdAndActiveTrue(request.spaceId()).isEmpty()) {
            throw new ResourceNotFoundException(Messages.Space.RESOURCE_TYPE, request.spaceId(),
                    Messages.Space.NOT_FOUND.formatted(request.spaceId()));
        }
        Instant from = request.from().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = request.to().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal availableHours = HOURS_PER_DAY.multiply(BigDecimal.valueOf(request.days()));

        return new OccupancyReportResponse(request.from(), request.to(),
                occupancyRepository.occupancy(request.spaceId(), from, to).stream()
                        .map(row -> toSpaceOccupancy(row, availableHours))
                        .toList());
    }

    private static SpaceOccupancy toSpaceOccupancy(OccupancyRow row, BigDecimal availableHours) {
        BigDecimal reservedHours = row.getReservedSeconds().divide(SECONDS_PER_HOUR, SCALE, RoundingMode.HALF_UP);
        BigDecimal percent = reservedHours.multiply(HUNDRED).divide(availableHours, SCALE, RoundingMode.HALF_UP);
        return new SpaceOccupancy(row.getSpaceId(), row.getSpaceName(), row.getConfirmedReservations(),
                reservedHours, availableHours, percent);
    }
}
