package com.cowork.booking.report.service;

import com.cowork.booking.common.AppConstants.Caches;
import com.cowork.booking.common.AppConstants.Messages;
import com.cowork.booking.common.ResourceNotFoundException;
import com.cowork.booking.report.dto.OccupancyReportRequest;
import com.cowork.booking.report.dto.OccupancyReportResponse;
import com.cowork.booking.report.mapper.OccupancyMapper;
import com.cowork.booking.report.repository.OccupancyRepository;
import com.cowork.booking.space.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OccupancyReportService {

    private static final BigDecimal HOURS_PER_DAY = BigDecimal.valueOf(24);

    private final OccupancyRepository occupancyRepository;
    private final SpaceRepository spaceRepository;
    private final OccupancyMapper occupancyMapper;

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
                        .map(row -> occupancyMapper.toSpaceOccupancy(row, availableHours))
                        .toList());
    }
}
