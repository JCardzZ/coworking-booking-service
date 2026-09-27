package com.cowork.booking.reservation.service;

import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Permissions;
import com.cowork.booking.common.BusinessRuleException;
import com.cowork.booking.common.ResourceNotFoundException;
import com.cowork.booking.common.UnprocessableOperationException;
import com.cowork.booking.reservation.dto.CreateReservationRequest;
import com.cowork.booking.reservation.dto.ReservationResponse;
import com.cowork.booking.reservation.mapper.ReservationMapper;
import com.cowork.booking.reservation.model.Reservation;
import com.cowork.booking.reservation.model.ReservationStatus;
import com.cowork.booking.reservation.repository.ReservationRepository;
import com.cowork.booking.space.model.Space;
import com.cowork.booking.space.model.SpaceType;
import com.cowork.booking.space.repository.SpaceRepository;
import com.cowork.booking.user.model.User;
import com.cowork.booking.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;

import static com.cowork.booking.user.UserFixtures.role;
import static com.cowork.booking.user.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");
    private static final Instant START = Instant.parse("2026-10-01T09:00:00Z");
    private static final Instant END = Instant.parse("2026-10-01T10:30:00Z");
    private static final String KEY = "key-12345678";

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private SpaceRepository spaceRepository;
    @Mock
    private UserRepository userRepository;

    private ReservationService service;
    private Space space;
    private User ana;

    @BeforeEach
    void setUp() {
        service = new ReservationService(reservationRepository, spaceRepository, userRepository, new ReservationMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        space = new Space("Sala Andes", SpaceType.MEETING_ROOM, 8, "Piso 2", new BigDecimal("25.00"));
        ReflectionTestUtils.setField(space, "id", 1L);
        ana = user(2L, "ana@coworking.com", role("USER"));
        authenticateAs(2L, Permissions.RESERVATION_CREATE, Permissions.RESERVATION_READ_OWN);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createChargesHourlyRateForTheBookedMinutes() {
        when(reservationRepository.findByUserIdAndIdempotencyKey(2L, KEY)).thenReturn(Optional.empty());
        when(spaceRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(space));
        when(reservationRepository.existsOverlapping(1L, START, END)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(ana));
        when(reservationRepository.saveAndFlush(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationService.CreateResult result = service.create(request(START, END), KEY);

        assertThat(result.created()).isTrue();
        assertThat(result.reservation().status()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        assertThat(result.reservation().totalAmount()).isEqualByComparingTo("37.50");
    }

    @Test
    void createRejectsOverlappingSlot() {
        when(reservationRepository.findByUserIdAndIdempotencyKey(2L, KEY)).thenReturn(Optional.empty());
        when(spaceRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(space));
        when(reservationRepository.existsOverlapping(1L, START, END)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(START, END), KEY))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo(ErrorCodes.RESERVATION_OVERLAP);
        verify(reservationRepository, never()).saveAndFlush(any());
    }

    @Test
    void concurrentOverlapCaughtByDatabaseIsReportedAsOverlap() {
        when(reservationRepository.findByUserIdAndIdempotencyKey(2L, KEY)).thenReturn(Optional.empty());
        when(spaceRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(space));
        when(reservationRepository.existsOverlapping(1L, START, END)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(ana));
        when(reservationRepository.saveAndFlush(any(Reservation.class))).thenThrow(new DataIntegrityViolationException("insert",
                new SQLException("conflicting key value violates exclusion constraint \"ex_reservations_no_overlap\"")));

        assertThatThrownBy(() -> service.create(request(START, END), KEY))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo(ErrorCodes.RESERVATION_OVERLAP);
    }

    @Test
    void createRejectsPastStart() {
        when(reservationRepository.findByUserIdAndIdempotencyKey(2L, KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(NOW.minusSeconds(60), END), KEY))
                .isInstanceOf(UnprocessableOperationException.class)
                .extracting("code").isEqualTo(ErrorCodes.RESERVATION_IN_PAST);
    }

    @Test
    void createRejectsReservationsLongerThanTwelveHours() {
        when(reservationRepository.findByUserIdAndIdempotencyKey(2L, KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(START, START.plusSeconds(13 * 3600)), KEY))
                .isInstanceOf(UnprocessableOperationException.class)
                .extracting("code").isEqualTo(ErrorCodes.RESERVATION_TOO_LONG);
    }

    @Test
    void createRejectsInactiveOrMissingSpace() {
        when(reservationRepository.findByUserIdAndIdempotencyKey(2L, KEY)).thenReturn(Optional.empty());
        when(spaceRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(START, END), KEY)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sameIdempotencyKeyReturnsTheOriginalReservation() {
        Reservation existing = reservation(ana);
        when(reservationRepository.findByUserIdAndIdempotencyKey(2L, KEY)).thenReturn(Optional.of(existing));

        ReservationService.CreateResult result = service.create(request(START, END), KEY);

        assertThat(result.created()).isFalse();
        assertThat(result.reservation().id()).isEqualTo(10L);
        verify(reservationRepository, never()).saveAndFlush(any());
    }

    @Test
    void sameIdempotencyKeyWithDifferentDataIsRejected() {
        when(reservationRepository.findByUserIdAndIdempotencyKey(2L, KEY)).thenReturn(Optional.of(reservation(ana)));

        assertThatThrownBy(() -> service.create(request(START, END.plusSeconds(3600)), KEY))
                .isInstanceOf(UnprocessableOperationException.class)
                .extracting("code").isEqualTo(ErrorCodes.IDEMPOTENCY_KEY_REUSED);
    }

    @Test
    void ownerCanCancelBeforeStart() {
        Reservation reservation = reservation(ana);
        when(reservationRepository.findWithDetailsById(10L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.saveAndFlush(reservation)).thenReturn(reservation);

        ReservationResponse response = service.cancel(10L);

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void otherUsersReservationIsReportedAsNotFound() {
        Reservation othersReservation = reservation(user(3L, "otro@coworking.com", role("USER")));
        when(reservationRepository.findWithDetailsById(10L)).thenReturn(Optional.of(othersReservation));

        assertThatThrownBy(() -> service.cancel(10L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.findById(10L)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(othersReservation.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
    }

    @Test
    void managerCanCancelAnyReservation() {
        authenticateAs(1L, Permissions.RESERVATION_MANAGE_ALL);
        Reservation reservation = reservation(ana);
        when(reservationRepository.findWithDetailsById(10L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.saveAndFlush(reservation)).thenReturn(reservation);

        assertThat(service.cancel(10L).status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    private Reservation reservation(User owner) {
        Reservation reservation = new Reservation(space, owner, START, END, new BigDecimal("37.50"), KEY);
        ReflectionTestUtils.setField(reservation, "id", 10L);
        return reservation;
    }

    private static CreateReservationRequest request(Instant start, Instant end) {
        return new CreateReservationRequest(1L, start, end);
    }

    private static void authenticateAs(Long userId, String... permissions) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(String.valueOf(userId)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
    }
}
