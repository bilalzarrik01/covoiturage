package ma.youcode.covoiturage.service.impl;

import java.util.List;
import java.util.NoSuchElementException;
import ma.youcode.covoiturage.dto.vehicule.VehiculeRequest;
import ma.youcode.covoiturage.dto.vehicule.VehiculeResponse;
import ma.youcode.covoiturage.entity.Conducteur;
import ma.youcode.covoiturage.entity.Vehicule;
import ma.youcode.covoiturage.mapper.VehiculeMapper;
import ma.youcode.covoiturage.repository.ConducteurRepository;
import ma.youcode.covoiturage.repository.VehiculeRepository;
import ma.youcode.covoiturage.service.VehiculeService;
import ma.youcode.covoiturage.repository.TrajetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehiculeServiceImpl implements VehiculeService {

    private final VehiculeRepository vehiculeRepository;
    private final ConducteurRepository conducteurRepository;
    private final VehiculeMapper vehiculeMapper;

    private final TrajetRepository trajetRepository;

    public VehiculeServiceImpl(
            VehiculeRepository vehiculeRepository,
            ConducteurRepository conducteurRepository,
            VehiculeMapper vehiculeMapper,
            TrajetRepository trajetRepository
    ) {
        this.vehiculeRepository = vehiculeRepository;
        this.conducteurRepository = conducteurRepository;
        this.vehiculeMapper = vehiculeMapper;
        this.trajetRepository = trajetRepository;
    }

    @Override
    public VehiculeResponse create(VehiculeRequest request) {
        Conducteur conducteur = getConducteurOrThrow(request.conducteurId());
        Vehicule vehicule = vehiculeMapper.toEntity(request);
        vehicule.setConducteur(conducteur);
        return vehiculeMapper.toResponse(vehiculeRepository.save(vehicule));
    }

    @Override
    public List<VehiculeResponse> findAll() {
        return vehiculeRepository.findAll().stream()
                .map(vehiculeMapper::toResponse)
                .toList();
    }

    @Override
    public VehiculeResponse findById(Long id) {
        return vehiculeMapper.toResponse(getVehiculeOrThrow(id));
    }

    @Override
    @Transactional
    public VehiculeResponse update(Long id, VehiculeRequest request) {
        Vehicule vehicule = vehiculeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NoSuchElementException("Véhicule introuvable : " + id));
        Conducteur conducteur = getConducteurOrThrow(request.conducteurId());
        if (trajetRepository.existsByVehiculeIdAndPlacesProposeesGreaterThan(id, request.capacite())) {
            throw new IllegalStateException("Un trajet propose plus de places que cette capacité.");
        }
        if (trajetRepository.existsByVehiculeIdAndConducteurIdNot(id, request.conducteurId())) {
            throw new IllegalStateException("Ce conducteur ne correspond pas aux trajets du véhicule.");
        }
        Vehicule updatedVehicule = vehiculeMapper.toEntity(request);

        vehicule.setMarque(updatedVehicule.getMarque());
        vehicule.setModele(updatedVehicule.getModele());
        vehicule.setImmatriculation(updatedVehicule.getImmatriculation());
        vehicule.setCouleur(updatedVehicule.getCouleur());
        vehicule.setCapacite(updatedVehicule.getCapacite());
        vehicule.setConducteur(conducteur);

        return vehiculeMapper.toResponse(vehiculeRepository.save(vehicule));
    }

    @Override
    public void delete(Long id) {
        vehiculeRepository.delete(getVehiculeOrThrow(id));
    }

    private Vehicule getVehiculeOrThrow(Long id) {
        return vehiculeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Vehicule introuvable pour l'identifiant : " + id
                ));
    }

    private Conducteur getConducteurOrThrow(Long id) {
        return conducteurRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Conducteur introuvable pour l'identifiant : " + id
                ));
    }
}
