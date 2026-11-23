package ma.youcode.covoiturage.entity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import ma.youcode.covoiturage.dto.pointarret.PointArretRequest;
import ma.youcode.covoiturage.dto.trajet.TrajetRequest;
import ma.youcode.covoiturage.dto.trajet.TrajetResponse;
import ma.youcode.covoiturage.mapper.PointArretMapper;
import ma.youcode.covoiturage.mapper.TrajetMapper;
import ma.youcode.covoiturage.repository.ConducteurRepository;
import ma.youcode.covoiturage.repository.PointArretRepository;
import ma.youcode.covoiturage.repository.ReservationRepository;
import ma.youcode.covoiturage.repository.TrajetRepository;
import ma.youcode.covoiturage.repository.VehiculeRepository;
import ma.youcode.covoiturage.service.impl.TrajetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

class TrajetStatusServiceTest {

    private final TrajetRepository trajets = mock(TrajetRepository.class);
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final PointArretRepository points = mock(PointArretRepository.class);
    private final ConducteurRepository conducteurs = mock(ConducteurRepository.class);
    private final VehiculeRepository vehicules = mock(VehiculeRepository.class);
    private final TrajetMapper mapper = mock(TrajetMapper.class);
    private final PointArretMapper pointMapper = mock(PointArretMapper.class);
    private final TrajetServiceImpl service = new TrajetServiceImpl(
            trajets, conducteurs, vehicules, points, reservations, mapper, pointMapper);
    private final Trajet trajet = new Trajet();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(trajet, "id", 1L);
        trajet.setStatut(StatutTrajet.PUBLIE);
        trajet.setDateDepart(LocalDateTime.now().minusHours(1));
        when(trajets.findByIdForUpdate(1L)).thenReturn(Optional.of(trajet));
        when(trajets.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(eq(trajet), anyList())).thenAnswer(invocation ->
                new TrajetResponse(1L, trajet.getDateDepart(), 4, trajet.getStatut(), 2L, 3L, List.of()));
    }

    @ParameterizedTest
    @EnumSource(value = StatutTrajet.class, names = {"PUBLIE", "COMPLET"})
    void completesActiveTripAfterDeparture(StatutTrajet initialStatus) {
        trajet.setStatut(initialStatus);
        assertThat(service.complete(1L).statut()).isEqualTo(StatutTrajet.TERMINE);
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.TERMINE);
        InOrder order = inOrder(trajets);
        order.verify(trajets).findByIdForUpdate(1L);
        order.verify(trajets).save(trajet);
        verifyNoInteractions(reservations);
    }

    @ParameterizedTest
    @EnumSource(value = StatutTrajet.class, names = {"PUBLIE", "COMPLET"})
    void rejectsCompletionBeforeDeparture(StatutTrajet initialStatus) {
        trajet.setStatut(initialStatus);
        trajet.setDateDepart(LocalDateTime.now().plusHours(1));
        assertThatIllegalStateException().isThrownBy(() -> service.complete(1L));
        assertThat(trajet.getStatut()).isEqualTo(initialStatus);
        verify(trajets, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatutTrajet.class, names = {"PUBLIE", "COMPLET"})
    void cancelsOnlyConfirmedReservationsOfThisTrip(StatutTrajet initialStatus) {
        trajet.setStatut(initialStatus);
        Reservation confirmed = reservation(trajet, StatutReservation.CONFIRMEE);
        Reservation alreadyCancelled = reservation(trajet, StatutReservation.ANNULEE);
        Trajet otherTrip = new Trajet();
        ReflectionTestUtils.setField(otherTrip, "id", 9L);
        Reservation other = reservation(otherTrip, StatutReservation.CONFIRMEE);
        when(reservations.findAll()).thenReturn(List.of(confirmed, alreadyCancelled, other));

        assertThat(service.cancel(1L).statut()).isEqualTo(StatutTrajet.ANNULE);
        assertThat(confirmed.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(alreadyCancelled.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(other.getStatut()).isEqualTo(StatutReservation.CONFIRMEE);
        InOrder order = inOrder(trajets, reservations);
        order.verify(trajets).findByIdForUpdate(1L);
        order.verify(reservations).findAll();
        order.verify(reservations).saveAll(List.of(confirmed));
        order.verify(trajets).save(trajet);
    }

    @Test
    void cancelsFutureTripWithoutReservations() {
        trajet.setDateDepart(LocalDateTime.now().plusDays(3));
        assertThat(service.cancel(1L).statut()).isEqualTo(StatutTrajet.ANNULE);
        verify(reservations).saveAll(List.of());
    }

    @ParameterizedTest
    @EnumSource(value = StatutTrajet.class, names = {"TERMINE", "ANNULE"})
    void terminalStatusesRejectEveryTransition(StatutTrajet status) {
        trajet.setStatut(status);
        assertThatIllegalStateException().isThrownBy(() -> service.complete(1L));
        assertThatIllegalStateException().isThrownBy(() -> service.cancel(1L));
        assertThat(trajet.getStatut()).isEqualTo(status);
        verify(trajets, never()).save(any());
        verifyNoInteractions(reservations);
    }

    @Test
    void completedTripCannotBeCompletedAgainOrCancelled() {
        service.complete(1L);
        assertThatIllegalStateException().isThrownBy(() -> service.complete(1L));
        assertThatIllegalStateException().isThrownBy(() -> service.cancel(1L));
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.TERMINE);
        verify(trajets, times(1)).save(trajet);
    }

    @Test
    void missingTripCannotChangeStatus() {
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> service.complete(99L));
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> service.cancel(99L));
        verify(trajets, never()).save(any());
        verifyNoInteractions(reservations);
    }

    @Test
    void validCreationDefaultsToPublie() {
        LocalDateTime departure = LocalDateTime.now().plusDays(2);
        Conducteur conducteur = new Conducteur();
        ReflectionTestUtils.setField(conducteur, "id", 2L);
        Vehicule vehicule = new Vehicule();
        vehicule.setConducteur(conducteur);
        vehicule.setCapacite(4);
        when(conducteurs.findById(2L)).thenReturn(Optional.of(conducteur));
        when(vehicules.findByIdForUpdate(3L)).thenReturn(Optional.of(vehicule));
        TrajetRequest request = new TrajetRequest(departure, 4, 2L, 3L, List.of(
                new PointArretRequest("Rabat", 1, departure, BigDecimal.ZERO),
                new PointArretRequest("Kenitra", 2, departure.plusHours(1), BigDecimal.TEN)));
        when(mapper.toEntity(request)).thenReturn(trajet);
        when(pointMapper.toEntity(any())).thenAnswer(invocation -> new PointArret());
        trajet.setStatut(null);

        assertThat(service.create(request).statut()).isEqualTo(StatutTrajet.PUBLIE);
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.PUBLIE);
    }

    private Reservation reservation(Trajet trip, StatutReservation status) {
        Reservation reservation = new Reservation();
        reservation.setTrajet(trip);
        reservation.setStatut(status);
        return reservation;
    }
}
