package ma.youcode.covoiturage.entity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import ma.youcode.covoiturage.dto.reservation.ReservationRequest;
import ma.youcode.covoiturage.mapper.ReservationMapper;
import ma.youcode.covoiturage.exception.ConflictException;
import ma.youcode.covoiturage.repository.PassagerRepository;
import ma.youcode.covoiturage.repository.PointArretRepository;
import ma.youcode.covoiturage.repository.ReservationRepository;
import ma.youcode.covoiturage.repository.TrajetRepository;
import ma.youcode.covoiturage.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

// In-memory entities and mocked repositories: these unit tests do not use Docker or a database.
class ReservationServiceTest {
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final TrajetRepository trajets = mock(TrajetRepository.class);
    private final PassagerRepository passagers = mock(PassagerRepository.class);
    private final PointArretRepository points = mock(PointArretRepository.class);
    private final ReservationMapper mapper = new ReservationMapperImpl();
    private final ReservationServiceImpl service = new ReservationServiceImpl(
            reservations, trajets, passagers, points, mapper);
    private final Trajet trajet = new Trajet();
    private final Passager passager = new Passager();
    private final PointArret montee = new PointArret();
    private final PointArret descente = new PointArret();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(trajet, "id", 1L);
        ReflectionTestUtils.setField(passager, "id", 2L);
        ReflectionTestUtils.setField(montee, "id", 3L);
        ReflectionTestUtils.setField(descente, "id", 4L);
        trajet.setStatut(StatutTrajet.PUBLIE);
        trajet.setDateDepart(LocalDateTime.now().plusDays(3));
        trajet.setPlacesProposees(4);
        montee.setTrajet(trajet);
        descente.setTrajet(trajet);
        montee.setOrdre(1);
        descente.setOrdre(2);
        montee.setPrixDepuisDepart(new BigDecimal("50.10"));
        descente.setPrixDepuisDepart(new BigDecimal("80.35"));
        when(trajets.findByIdForUpdate(1L)).thenReturn(Optional.of(trajet));
        when(passagers.findById(2L)).thenReturn(Optional.of(passager));
        when(points.findById(3L)).thenReturn(Optional.of(montee));
        when(points.findById(4L)).thenReturn(Optional.of(descente));
        when(reservations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsConfirmedReservationWithExactSegmentPriceAndTimestamp() {
        LocalDateTime before = LocalDateTime.now();
        var response = service.create(request(2));
        assertThat(response.montantTotal()).isEqualByComparingTo("60.50");
        assertThat(response.nombrePlaces()).isEqualTo(2);
        assertThat(response.dateReservation()).isBetween(before, LocalDateTime.now());
        assertThat(response.statut()).isEqualTo(StatutReservation.CONFIRMEE);
        assertThat(response.passagerId()).isEqualTo(2L);
        assertThat(response.trajetId()).isEqualTo(1L);
        assertThat(response.pointMonteeId()).isEqualTo(3L);
        assertThat(response.pointDescenteId()).isEqualTo(4L);
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.PUBLIE);
        InOrder order = inOrder(trajets, reservations);
        order.verify(trajets).findByIdForUpdate(1L);
        order.verify(reservations).existsByPassagerIdAndTrajetIdAndStatut(2L, 1L, StatutReservation.CONFIRMEE);
        order.verify(reservations).sumNombrePlacesByTrajetIdAndStatut(1L, StatutReservation.CONFIRMEE);
        order.verify(reservations).save(any());
    }

    @Test
    void lastSeatsMarkTripComplete() {
        when(reservations.sumNombrePlacesByTrajetIdAndStatut(1L, StatutReservation.CONFIRMEE)).thenReturn(2L);
        service.create(request(2));
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.COMPLET);
    }

    @Test
    void rejectsInsufficientCapacity() {
        when(reservations.sumNombrePlacesByTrajetIdAndStatut(1L, StatutReservation.CONFIRMEE)).thenReturn(3L);
        assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> service.create(request(2)));
        verify(reservations, never()).save(any());
    }

    @Test
    void rejectsDuplicateConfirmedReservation() {
        when(reservations.existsByPassagerIdAndTrajetIdAndStatut(2L, 1L, StatutReservation.CONFIRMEE))
                .thenReturn(true);
        assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> service.create(request(1)));
        verify(reservations, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidSeatCount(int seats) {
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(request(seats)));
        verifyNoInteractions(trajets);
    }

    @Test
    void rejectsIncompleteRequest() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(null));
        assertThatIllegalArgumentException().isThrownBy(() ->
                service.create(new ReservationRequest(2L, 1L, 3L, 4L, null)));
        verifyNoInteractions(trajets);
    }

    @ParameterizedTest
    @EnumSource(value = StatutTrajet.class, names = {"COMPLET", "ANNULE", "TERMINE"})
    void rejectsUnpublishedTrip(StatutTrajet status) {
        trajet.setStatut(status);
        assertThatIllegalStateException().isThrownBy(() -> service.create(request(1)));
        verify(reservations, never()).save(any());
    }

    @Test
    void rejectsPastDeparture() {
        trajet.setDateDepart(LocalDateTime.now().minusSeconds(1));
        assertThatIllegalStateException().isThrownBy(() -> service.create(request(1)));
    }

    @Test
    void rejectsStopsOfAnotherTrip() {
        Trajet other = new Trajet();
        ReflectionTestUtils.setField(other, "id", 99L);
        montee.setTrajet(other);
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(request(1)));
        montee.setTrajet(trajet);
        descente.setTrajet(other);
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(request(1)));
        verify(reservations, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 0})
    void rejectsIdenticalOrReversedStopOrder(int destinationOrder) {
        descente.setOrdre(destinationOrder);
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(request(1)));
        verify(reservations, never()).save(any());
    }

    @Test
    void rejectsMissingPassengerTripAndStop() {
        when(passagers.findById(2L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> service.create(request(1)));
        when(passagers.findById(2L)).thenReturn(Optional.of(passager));
        when(points.findById(3L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> service.create(request(1)));
        when(trajets.findByIdForUpdate(1L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> service.create(request(1)));
        verify(reservations, never()).save(any());
    }

    @Test
    void cancellationReopensFullTripAndLocksBeforeLoadingReservation() {
        Reservation reservation = existingReservation();
        trajet.setStatut(StatutTrajet.COMPLET);
        when(reservations.sumNombrePlacesByTrajetIdAndStatut(1L, StatutReservation.CONFIRMEE)).thenReturn(4L);
        assertThat(service.cancel(5L).statut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.PUBLIE);
        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        InOrder order = inOrder(trajets, reservations);
        order.verify(reservations).findTrajetIdById(5L);
        order.verify(trajets).findByIdForUpdate(1L);
        order.verify(reservations).findById(5L);
    }

    @Test
    void rejectsCancellationWithin24Hours() {
        Reservation reservation = existingReservation();
        trajet.setDateDepart(LocalDateTime.now().plusHours(23));
        assertThatIllegalStateException().isThrownBy(() -> service.cancel(5L));
        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.CONFIRMEE);
        verify(reservations, never()).save(any());
    }

    @Test
    void rejectsRepeatedCancellation() {
        existingReservation().setStatut(StatutReservation.ANNULEE);
        assertThatIllegalStateException().isThrownBy(() -> service.cancel(5L));
        verify(reservations, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatutTrajet.class, names = {"ANNULE", "TERMINE"})
    void cancellationDoesNotRepublishInactiveTrip(StatutTrajet status) {
        existingReservation();
        trajet.setStatut(status);
        service.cancel(5L);
        assertThat(trajet.getStatut()).isEqualTo(status);
    }

    @Test
    void retrievesAllOneAndPassengerReservations() {
        Reservation reservation = existingReservation();
        when(reservations.findAll()).thenReturn(List.of(reservation));
        when(reservations.findAllByPassagerId(2L)).thenReturn(List.of(reservation));
        assertThat(service.findById(5L).id()).isEqualTo(5L);
        assertThat(service.findAll()).containsExactly(mapper.toResponse(reservation));
        assertThat(service.findByPassagerId(2L)).containsExactly(mapper.toResponse(reservation));
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> service.findById(99L));
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> service.cancel(99L));
    }

    private ReservationRequest request(int seats) {
        return new ReservationRequest(2L, 1L, 3L, 4L, seats);
    }

    private Reservation existingReservation() {
        Reservation reservation = mapper.toEntity(request(2));
        ReflectionTestUtils.setField(reservation, "id", 5L);
        reservation.setTrajet(trajet);
        reservation.setPassager(passager);
        reservation.setPointMontee(montee);
        reservation.setPointDescente(descente);
        reservation.setStatut(StatutReservation.CONFIRMEE);
        when(reservations.findTrajetIdById(5L)).thenReturn(Optional.of(1L));
        when(reservations.findById(5L)).thenReturn(Optional.of(reservation));
        return reservation;
    }
}
