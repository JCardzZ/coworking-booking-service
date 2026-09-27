package com.cowork.booking.report.dto;

import java.time.LocalDate;
import java.util.List;

public record OccupancyReportResponse(LocalDate from, LocalDate to, List<SpaceOccupancy> spaces) {
}
