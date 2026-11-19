package ma.youcode.covoiturage.service;

import java.util.List;
import ma.youcode.covoiturage.dto.conducteur.ConducteurRequest;
import ma.youcode.covoiturage.dto.conducteur.ConducteurResponse;

public interface ConducteurService {

    ConducteurResponse create(ConducteurRequest request);

    List<ConducteurResponse> findAll();

    ConducteurResponse findById(Long id);

    ConducteurResponse update(Long id, ConducteurRequest request);

    void delete(Long id);
}
