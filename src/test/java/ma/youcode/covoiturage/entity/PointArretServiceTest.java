package ma.youcode.covoiturage.entity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import ma.youcode.covoiturage.dto.pointarret.PointArretRequest;
import ma.youcode.covoiturage.mapper.PointArretMapper;
import ma.youcode.covoiturage.repository.PointArretRepository;
import ma.youcode.covoiturage.repository.ReservationRepository;
import ma.youcode.covoiturage.repository.TrajetRepository;
import ma.youcode.covoiturage.service.impl.PointArretServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PointArretServiceTest {
    @Mock PointArretRepository points;
    @Mock ReservationRepository reservations;
    @Mock TrajetRepository trajets;
    @Spy PointArretMapper mapper = new PointArretMapperImpl();
    @InjectMocks PointArretServiceImpl service;

    private final LocalDateTime depart = LocalDateTime.now().plusDays(3);
    private final PointArretRequest rabat = new PointArretRequest("Rabat", 1, depart, BigDecimal.ZERO);
    private final PointArretRequest kenitra = new PointArretRequest("Kenitra", 2, depart.plusHours(1), BigDecimal.TEN);

    @BeforeEach
    void trajetPublie() {
        Trajet trajet = new Trajet();
        ReflectionTestUtils.setField(trajet, "id", 1L);
        trajet.setStatut(StatutTrajet.PUBLIE);
        trajet.setDateDepart(depart);
        when(trajets.findByIdForUpdate(1L)).thenReturn(Optional.of(trajet));
    }

    private PointArret arret(long id, PointArretRequest request) {
        PointArret point = mapper.toEntity(request);
        ReflectionTestUtils.setField(point, "id", id);
        return point;
    }

    @Test
    void refuseDeSupprimerLeDeuxiemeArret() {
        PointArret premier = arret(10L, rabat);
        PointArret dernier = arret(11L, kenitra);
        when(points.findTrajetIdById(11L)).thenReturn(Optional.of(1L));
        when(points.findById(11L)).thenReturn(Optional.of(dernier));
        when(points.findAllByTrajetIdIn(List.of(1L))).thenReturn(List.of(premier, dernier));

        assertThatIllegalArgumentException().isThrownBy(() -> service.delete(11L)).withMessageContaining("deux");
        verify(points, never()).delete(any());
    }

    @Test
    void refuseDeModifierUnArretReserve() {
        PointArret reserve = arret(11L, kenitra);
        when(points.findTrajetIdById(11L)).thenReturn(Optional.of(1L));
        when(points.findById(11L)).thenReturn(Optional.of(reserve));
        when(reservations.usesStop(11L, StatutReservation.CONFIRMEE)).thenReturn(true);

        assertThatIllegalStateException().isThrownBy(() -> service.update(11L, rabat))
                .withMessageContaining("réservation confirmée");
        verify(points, never()).save(any());
    }

    @Test
    void refuseUnNouvelArretDontLePrixDiminue() {
        List<PointArret> existants = List.of(arret(10L, rabat), arret(11L, kenitra));
        when(points.findAllByTrajetIdIn(List.of(1L))).thenReturn(existants);
        var nouveau = new PointArretRequest("Tanger", 3, depart.plusHours(2), BigDecimal.ONE);

        assertThatIllegalArgumentException().isThrownBy(() -> service.create(1L, nouveau))
                .withMessageContaining("prix cumulés");
        verify(points, never()).save(any());
    }
}
