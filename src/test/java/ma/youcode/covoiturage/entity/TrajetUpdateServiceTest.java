package ma.youcode.covoiturage.entity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import ma.youcode.covoiturage.dto.pointarret.PointArretRequest;
import ma.youcode.covoiturage.dto.trajet.TrajetRequest;
import ma.youcode.covoiturage.mapper.PointArretMapper;
import ma.youcode.covoiturage.mapper.TrajetMapper;
import ma.youcode.covoiturage.repository.*;
import ma.youcode.covoiturage.service.impl.TrajetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrajetUpdateServiceTest {
    @Mock TrajetRepository trajets;
    @Mock ReservationRepository reservations;
    @Mock ConducteurRepository conducteurs;
    @Mock VehiculeRepository vehicules;
    @Mock PointArretRepository points;
    @Mock TrajetMapper trajetMapper;
    @Mock PointArretMapper pointMapper;
    @InjectMocks TrajetServiceImpl service;

    private final Trajet trajet = new Trajet();
    private final LocalDateTime depart = LocalDateTime.now().plusDays(3);

    @BeforeEach
    void prepareVehicule() {
        Conducteur conducteur = new Conducteur();
        Vehicule vehicule = new Vehicule();
        vehicule.setConducteur(conducteur);
        vehicule.setCapacite(4);
        when(conducteurs.findById(2L)).thenReturn(Optional.of(conducteur));
        when(vehicules.findByIdForUpdate(3L)).thenReturn(Optional.of(vehicule));
    }

    private TrajetRequest demande(int places, LocalDateTime montee) {
        return new TrajetRequest(depart, places, 2L, 3L, List.of(
                new PointArretRequest("Rabat", 1, montee, BigDecimal.ZERO),
                new PointArretRequest("Kenitra", 2, depart.plusHours(1), BigDecimal.TEN)));
    }

    @Test
    void refuseUneCapaciteInferieureAuxPlacesConfirmees() {
        trajet.setStatut(StatutTrajet.PUBLIE);
        when(trajets.findByIdForUpdate(1L)).thenReturn(Optional.of(trajet));
        when(reservations.sumNombrePlacesByTrajetIdAndStatut(1L, StatutReservation.CONFIRMEE)).thenReturn(3L);

        assertThatIllegalStateException().isThrownBy(() -> service.update(1L, demande(2, depart)))
                .withMessageContaining("réservées");
        verify(trajets, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"PUBLIE,2,COMPLET", "COMPLET,4,PUBLIE"})
    void adapteLeStatutALaCapacite(StatutTrajet initial, int places, StatutTrajet attendu) {
        trajet.setStatut(initial);
        when(trajets.findByIdForUpdate(1L)).thenReturn(Optional.of(trajet));
        when(reservations.sumNombrePlacesByTrajetIdAndStatut(1L, StatutReservation.CONFIRMEE)).thenReturn(2L);
        List<PointArret> arrets = demande(places, depart).pointsArret().stream().map(p -> {
            PointArret arret = new PointArret();
            arret.setVille(p.ville());
            arret.setOrdre(p.ordre());
            arret.setHeurePassage(p.heurePassage());
            arret.setPrixDepuisDepart(p.prixDepuisDepart());
            return arret;
        }).toList();
        when(points.findAllByTrajetIdIn(List.of(1L))).thenReturn(arrets);

        service.update(1L, demande(places, depart));

        assertThat(trajet.getStatut()).isEqualTo(attendu);
        assertThat(trajet.getPlacesProposees()).isEqualTo(places);
        verify(points, never()).deleteAll(anyList());
    }

    @ParameterizedTest
    @EnumSource(value = StatutTrajet.class, names = {"TERMINE", "ANNULE"})
    void refuseLaModificationDunTrajetCloture(StatutTrajet statut) {
        trajet.setStatut(statut);
        when(trajets.findByIdForUpdate(1L)).thenReturn(Optional.of(trajet));

        assertThatIllegalStateException().isThrownBy(() -> service.update(1L, demande(4, depart)));
        verify(trajets, never()).save(any());
    }

    @Test
    void refuseUnArretAvantLeDepart() {
        TrajetRequest demande = demande(4, depart.minusMinutes(1));

        assertThatIllegalArgumentException().isThrownBy(() -> service.create(demande))
                .withMessageContaining("précéder");
        verify(trajets, never()).save(any());
    }
}
