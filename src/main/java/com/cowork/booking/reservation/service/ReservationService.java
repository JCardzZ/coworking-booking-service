package com.cowork.booking.reservation.service;

import com.cowork.booking.common.AppConstants.Audit;
import com.cowork.booking.common.AppConstants.Caches;
import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Limits;
import com.cowork.booking.common.AppConstants.Messages;
import com.cowork.booking.common.AppConstants.Permissions;
import com.cowork.booking.common.Audited;
import com.cowork.booking.common.BusinessRuleException;
import com.cowork.booking.common.CurrentUser;
import com.cowork.booking.common.ResourceNotFoundException;
import com.cowork.booking.common.UnprocessableOperationException;
import com.cowork.booking.reservation.client.PaymentClient;
import com.cowork.booking.reservation.client.PaymentRequest;
import com.cowork.booking.reservation.client.PaymentResult;
import com.cowork.booking.reservation.dto.CreateReservationRequest;
import com.cowork.booking.reservation.dto.ReservationFilter;
import com.cowork.booking.reservation.dto.ReservationResponse;
import com.cowork.booking.reservation.event.ReservationConfirmedEvent;
import com.cowork.booking.reservation.mapper.ReservationMapper;
import com.cowork.booking.reservation.model.Reservation;
import com.cowork.booking.reservation.repository.ReservationRepository;
import com.cowork.booking.reservation.repository.ReservationSpecifications;
import com.cowork.booking.space.model.Space;
import com.cowork.booking.space.repository.SpaceRepository;
import com.cowork.booking.user.model.User;
import com.cowork.booking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private static final String OVERLAP_CONSTRAINT = "ex_reservations_no_overlap";
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private final ReservationRepository reservationRepository;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final ReservationMapper reservationMapper;
    private final PaymentClient paymentClient;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // created=false when it's a replay of the same Idempotency-Key
    public record CreateResult(ReservationResponse reservation, boolean created) {
    }

    @Transactional
    @Audited(Audit.RESERVATION_CREATE)
    public CreateResult create(CreateReservationRequest request, String idempotencyKey) {
        Long userId = CurrentUser.id().orElseThrow();
        Optional<Reservation> previous = reservationRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (previous.isPresent()) {
            return replay(previous.get(), request);
        }
        validatePeriod(request.startAt(), request.endAt());
        Space space = spaceRepository.findByIdAndActiveTrue(request.spaceId())
                .orElseThrow(() -> new ResourceNotFoundException(Messages.Space.RESOURCE_TYPE, request.spaceId(),
                        Messages.Space.NOT_FOUND.formatted(request.spaceId())));
        if (reservationRepository.existsOverlapping(space.getId(), request.startAt(), request.endAt())) {
            throw overlap();
        }
        User user = userRepository.findById(userId).orElseThrow();
        Reservation reservation = new Reservation(space, user, request.startAt(), request.endAt(),
                price(space, request.startAt(), request.endAt()), idempotencyKey);
        return new CreateResult(reservationMapper.toResponse(save(reservation)), true);
    }

    // confirmed=false: payment provider unavailable, the reservation stays PENDING_PAYMENT and can be retried
    public record ConfirmResult(ReservationResponse reservation, boolean confirmed) {
    }

    // no DB transaction open while we wait for the payment provider
    @Audited(Audit.RESERVATION_CONFIRM)
    @CacheEvict(cacheNames = Caches.OCCUPANCY_REPORT, allEntries = true)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ConfirmResult confirm(Long id) {
        Reservation reservation = getVisible(id, Permissions.RESERVATION_MANAGE_ALL);
        reservation.checkCanConfirm();
        PaymentResult payment = paymentClient.validate(
                new PaymentRequest(reservation.getId(), reservation.getUser().getId(), reservation.getTotalAmount()));
        return switch (payment) {
            case APPROVED -> new ConfirmResult(markConfirmed(id), true);
            case DECLINED -> throw new UnprocessableOperationException(ErrorCodes.PAYMENT_DECLINED,
                    Messages.Reservation.PAYMENT_DECLINED);
            case UNAVAILABLE -> new ConfirmResult(reservationMapper.toResponse(reservation), false);
        };
    }

    public Page<ReservationResponse> findAll(ReservationFilter filter, Pageable pageable) {
        Long ownerId = CurrentUser.hasAuthority(Permissions.RESERVATION_READ_ALL)
                ? filter.userId()
                : CurrentUser.id().orElseThrow();
        return reservationRepository.findAll(ReservationSpecifications.matching(filter, ownerId), pageable)
                .map(reservationMapper::toResponse);
    }

    public ReservationResponse findById(Long id) {
        return reservationMapper.toResponse(getVisible(id, Permissions.RESERVATION_READ_ALL));
    }

    @Transactional
    @Audited(Audit.RESERVATION_CANCEL)
    @CacheEvict(cacheNames = Caches.OCCUPANCY_REPORT, allEntries = true)
    public ReservationResponse cancel(Long id) {
        Reservation reservation = getVisible(id, Permissions.RESERVATION_MANAGE_ALL);
        reservation.cancel(clock.instant());
        return reservationMapper.toResponse(reservationRepository.saveAndFlush(reservation));
    }

    private ReservationResponse markConfirmed(Long id) {
        return transactionTemplate.execute(tx -> {
            Reservation reservation = reservationRepository.findWithDetailsById(id).orElseThrow(() -> notFound(id));
            reservation.confirm();
            Reservation confirmed = reservationRepository.saveAndFlush(reservation);
            eventPublisher.publishEvent(ReservationConfirmedEvent.of(confirmed));
            return reservationMapper.toResponse(confirmed);
        });
    }

    private CreateResult replay(Reservation previous, CreateReservationRequest request) {
        if (!previous.isSameRequest(request.spaceId(), request.startAt(), request.endAt())) {
            throw new UnprocessableOperationException(ErrorCodes.IDEMPOTENCY_KEY_REUSED,
                    Messages.Reservation.IDEMPOTENCY_KEY_REUSED);
        }
        return new CreateResult(reservationMapper.toResponse(previous), false);
    }

    private void validatePeriod(Instant startAt, Instant endAt) {
        if (!startAt.isAfter(clock.instant())) {
            throw new UnprocessableOperationException(ErrorCodes.RESERVATION_IN_PAST, Messages.Reservation.IN_PAST);
        }
        if (Duration.between(startAt, endAt).compareTo(Duration.ofHours(Limits.RESERVATION_MAX_HOURS)) > 0) {
            throw new UnprocessableOperationException(ErrorCodes.RESERVATION_TOO_LONG,
                    Messages.Reservation.TOO_LONG.formatted(Limits.RESERVATION_MAX_HOURS));
        }
    }

    private static BigDecimal price(Space space, Instant startAt, Instant endAt) {
        BigDecimal minutes = BigDecimal.valueOf(Duration.between(startAt, endAt).toMinutes());
        return space.getHourlyRate().multiply(minutes).divide(MINUTES_PER_HOUR, Limits.MONEY_FRACTION_DIGITS, RoundingMode.HALF_UP);
    }

    // the pre-check can't catch two requests at the same time, the DB constraint does
    private Reservation save(Reservation reservation) {
        try {
            return reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException ex) {
            if (String.valueOf(ex.getMostSpecificCause().getMessage()).contains(OVERLAP_CONSTRAINT)) {
                throw overlap();
            }
            throw ex;
        }
    }

    // 404 instead of 403 so we don't leak other users' reservation ids
    private Reservation getVisible(Long id, String allAccessPermission) {
        Reservation reservation = reservationRepository.findWithDetailsById(id)
                .orElseThrow(() -> notFound(id));
        boolean allowed = CurrentUser.hasAuthority(allAccessPermission)
                || CurrentUser.id().filter(reservation::isOwnedBy).isPresent();
        if (!allowed) {
            throw notFound(id);
        }
        return reservation;
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException(Messages.Reservation.RESOURCE_TYPE, id, Messages.Reservation.NOT_FOUND.formatted(id));
    }

    private static BusinessRuleException overlap() {
        return new BusinessRuleException(ErrorCodes.RESERVATION_OVERLAP, Messages.Reservation.OVERLAP);
    }
}
