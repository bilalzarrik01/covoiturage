package ma.youcode.covoiturage.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ma.youcode.covoiturage.dto.trajet.TrajetResponse;
import ma.youcode.covoiturage.entity.PointArret;
import ma.youcode.covoiturage.entity.StatutTrajet;
import ma.youcode.covoiturage.entity.Trajet;
import ma.youcode.covoiturage.mapper.PointArretMapper;
import ma.youcode.covoiturage.mapper.TrajetMapper;
import ma.youcode.covoiturage.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class TrajetSearchServiceTest {
    private static final LocalDate DATE = LocalDate.of(2030, 6, 15);
    private static final PageRequest PAGE = PageRequest.of(1, 2);
    @Mock TrajetRepository trajetRepository;
    @Mock ConducteurRepository conducteurRepository;
    @Mock VehiculeRepository vehiculeRepository;
    @Mock PointArretRepository pointArretRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock TrajetMapper trajetMapper;
    @Mock PointArretMapper pointArretMapper;
    @InjectMocks TrajetServiceImpl service;

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidSeatCountBeforeQuerying(int places) {
        assertThatIllegalArgumentException().isThrownBy(() ->
                service.search("Rabat", "Kenitra", DATE, places, null, PAGE));
        verifyNoInteractions(trajetRepository, pointArretRepository);
    }

    @Test
    void rejectsNegativePriceBeforeQuerying() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                service.search("Rabat", "Kenitra", DATE, 1, new BigDecimal("-0.01"), PAGE));
        verifyNoInteractions(trajetRepository, pointArretRepository);
    }

    @Test
    void mapsOnlyTheSelectedPagesStopsAndPreservesPagination() {
        Trajet trajet = mock(Trajet.class);
        PointArret stop = mock(PointArret.class);
        when(trajet.getId()).thenReturn(7L);
        when(stop.getTrajet()).thenReturn(trajet);
        TrajetResponse response = new TrajetResponse(7L, DATE.atTime(10, 0), 4,
                StatutTrajet.PUBLIE, 1L, 1L, List.of());
        when(trajetRepository.search("Rabat", "Kenitra", DATE.atStartOfDay(),
                DATE.plusDays(1).atStartOfDay(), 2, new BigDecimal("30"), PAGE))
                .thenReturn(new PageImpl<>(List.of(trajet), PAGE, 3));
        when(pointArretRepository.findAllByTrajetIdIn(List.of(7L))).thenReturn(List.of(stop));
        when(trajetMapper.toResponse(trajet, List.of(stop))).thenReturn(response);

        Page<TrajetResponse> result = service.search(" Rabat ", " Kenitra ", DATE,
                2, new BigDecimal("30"), PAGE);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getNumber()).isEqualTo(1);
        verify(pointArretRepository).findAllByTrajetIdIn(List.of(7L));
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void acceptsZeroPriceAndDoesNotLoadStopsForAnEmptyPage() {
        when(trajetRepository.search(any(), any(), any(), any(), org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(BigDecimal.ZERO), any())).thenReturn(Page.empty(PAGE));

        assertThat(service.search("Rabat", "Kenitra", DATE, 1, BigDecimal.ZERO, PAGE)).isEmpty();
        verifyNoInteractions(pointArretRepository, trajetMapper);
    }
}
