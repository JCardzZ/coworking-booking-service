package com.cowork.booking.report.service;

import com.cowork.booking.common.ResourceNotFoundException;
import com.cowork.booking.report.dto.OccupancyReportRequest;
import com.cowork.booking.report.dto.SpaceOccupancy;
import com.cowork.booking.report.repository.OccupancyRepository;
import com.cowork.booking.report.repository.OccupancyRow;
import com.cowork.booking.space.repository.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OccupancyReportServiceTest {

    private static final LocalDate FROM = LocalDate.parse("2027-04-01");
    private static final LocalDate TO = LocalDate.parse("2027-04-02");

    @Mock
    private OccupancyRepository occupancyRepository;
    @Mock
    private SpaceRepository spaceRepository;

    private OccupancyReportService service;

    @BeforeEach
    void setUp() {
        service = new OccupancyReportService(occupancyRepository, spaceRepository);
    }

    @Test
    void percentIsReservedHoursOverTheHoursOfTheWholeRange() {
        // 12 h reserved out of 48 h (two full days, UTC)
        when(occupancyRepository.occupancy(null, Instant.parse("2027-04-01T00:00:00Z"), Instant.parse("2027-04-03T00:00:00Z")))
                .thenReturn(List.of(row(1L, "Sala Andes", 3, 43_200)));

        SpaceOccupancy occupancy = service.occupancy(new OccupancyReportRequest(FROM, TO, null)).spaces().getFirst();

        assertThat(occupancy.reservedHours()).isEqualByComparingTo("12");
        assertThat(occupancy.availableHours()).isEqualByComparingTo("48");
        assertThat(occupancy.occupancyPercent()).isEqualByComparingTo("25.00");
        assertThat(occupancy.confirmedReservations()).isEqualTo(3);
    }

    @Test
    void spaceWithoutReservationsIsZero() {
        when(occupancyRepository.occupancy(any(), any(), any())).thenReturn(List.of(row(2L, "Escritorio", 0, 0)));

        SpaceOccupancy occupancy = service.occupancy(new OccupancyReportRequest(FROM, TO, null)).spaces().getFirst();

        assertThat(occupancy.occupancyPercent()).isEqualByComparingTo("0");
    }

    @Test
    void unknownSpaceIsNotFound() {
        when(spaceRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.occupancy(new OccupancyReportRequest(FROM, TO, 99L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(occupancyRepository, never()).occupancy(any(), any(), any());
    }

    @Test
    void rangeMustBeOrderedAndAtMostOneYear() {
        assertThat(new OccupancyReportRequest(FROM, FROM, null).isValidRange()).isTrue();
        assertThat(new OccupancyReportRequest(TO, FROM, null).isValidRange()).isFalse();
        assertThat(new OccupancyReportRequest(FROM, FROM.plusDays(365), null).isValidRange()).isTrue();
        assertThat(new OccupancyReportRequest(FROM, FROM.plusDays(366), null).isValidRange()).isFalse();
    }

    private static OccupancyRow row(Long spaceId, String name, long reservations, long seconds) {
        return new OccupancyRow() {
            public Long getSpaceId() { return spaceId; }
            public String getSpaceName() { return name; }
            public Long getConfirmedReservations() { return reservations; }
            public BigDecimal getReservedSeconds() { return BigDecimal.valueOf(seconds); }
        };
    }
}
