package ma.youcode.covoiturage.service;

import java.util.List;
import ma.youcode.covoiturage.dto.vehicule.VehiculeRequest;
import ma.youcode.covoiturage.dto.vehicule.VehiculeResponse;

public interface VehiculeService {

    VehiculeResponse create(VehiculeRequest request);

    List<VehiculeResponse> findAll();

    VehiculeResponse findById(Long id);

    VehiculeResponse update(Long id, VehiculeRequest request);

    void delete(Long id);
}
