package com.cowork.booking.report.repository;

import java.math.BigDecimal;

public interface OccupancyRow {

    Long getSpaceId();

    String getSpaceName();

    Long getConfirmedReservations();

    BigDecimal getReservedSeconds();
}
