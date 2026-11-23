package ma.youcode.covoiturage.entity;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.*;

import java.util.Optional;
import ma.youcode.covoiturage.dto.vehicule.VehiculeRequest;
import ma.youcode.covoiturage.mapper.VehiculeMapper;
import ma.youcode.covoiturage.repository.ConducteurRepository;
import ma.youcode.covoiturage.repository.TrajetRepository;
import ma.youcode.covoiturage.repository.VehiculeRepository;
import ma.youcode.covoiturage.service.impl.VehiculeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehiculeServiceTest {
    @Mock VehiculeRepository vehicules;
    @Mock ConducteurRepository conducteurs;
    @Mock TrajetRepository trajets;
    @Mock VehiculeMapper mapper;
    @InjectMocks VehiculeServiceImpl service;

    private final VehiculeRequest demande = new VehiculeRequest("Dacia", "Logan", "D-123", "Blanc", 2, 2L);

    @BeforeEach
    void vehiculeEtConducteurExistants() {
        when(vehicules.findByIdForUpdate(1L)).thenReturn(Optional.of(new Vehicule()));
        when(conducteurs.findById(2L)).thenReturn(Optional.of(new Conducteur()));
    }

    @Test
    void refuseUneCapaciteInsuffisantePourUnTrajet() {
        when(trajets.existsByVehiculeIdAndPlacesProposeesGreaterThan(1L, 2)).thenReturn(true);

        assertThatIllegalStateException().isThrownBy(() -> service.update(1L, demande))
                .withMessageContaining("capacité");
        verify(vehicules, never()).save(any());
    }

    @Test
    void refuseUnProprietaireDifferentDuConducteurDuTrajet() {
        when(trajets.existsByVehiculeIdAndConducteurIdNot(1L, 2L)).thenReturn(true);

        assertThatIllegalStateException().isThrownBy(() -> service.update(1L, demande))
                .withMessageContaining("conducteur");
        verify(vehicules, never()).save(any());
    }
}
