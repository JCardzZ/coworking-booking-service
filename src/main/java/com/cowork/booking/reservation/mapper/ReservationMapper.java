package com.cowork.booking.reservation.mapper;

import com.cowork.booking.reservation.dto.ReservationResponse;
import com.cowork.booking.reservation.model.Reservation;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(reservation.getId(), reservation.getSpace().getId(), reservation.getSpace().getName(),
                reservation.getUser().getId(), reservation.getUser().getEmail(), reservation.getStartAt(),
                reservation.getEndAt(), reservation.getStatus(), reservation.getTotalAmount(), reservation.getCreatedAt(),
                reservation.getCancelledAt(), reservation.getPaymentReference());
    }
}
