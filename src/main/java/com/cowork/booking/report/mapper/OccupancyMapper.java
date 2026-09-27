package com.cowork.booking.report.mapper;

import com.cowork.booking.report.dto.SpaceOccupancy;
import com.cowork.booking.report.repository.OccupancyRow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class OccupancyMapper {

    private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int SCALE = 2;

    public SpaceOccupancy toSpaceOccupancy(OccupancyRow row, BigDecimal availableHours) {
        BigDecimal reservedHours = row.getReservedSeconds().divide(SECONDS_PER_HOUR, SCALE, RoundingMode.HALF_UP);
        BigDecimal percent = reservedHours.multiply(HUNDRED).divide(availableHours, SCALE, RoundingMode.HALF_UP);
        return new SpaceOccupancy(row.getSpaceId(), row.getSpaceName(), row.getConfirmedReservations(),
                reservedHours, availableHours, percent);
    }
}
