package ma.youcode.covoiturage.service.impl;

import java.util.List;
import java.util.NoSuchElementException;
import ma.youcode.covoiturage.dto.conducteur.ConducteurRequest;
import ma.youcode.covoiturage.dto.conducteur.ConducteurResponse;
import ma.youcode.covoiturage.entity.Conducteur;
import ma.youcode.covoiturage.mapper.ConducteurMapper;
import ma.youcode.covoiturage.repository.ConducteurRepository;
import ma.youcode.covoiturage.service.ConducteurService;
import org.springframework.stereotype.Service;

@Service
public class ConducteurServiceImpl implements ConducteurService {

    private final ConducteurRepository conducteurRepository;
    private final ConducteurMapper conducteurMapper;

    public ConducteurServiceImpl(
            ConducteurRepository conducteurRepository,
            ConducteurMapper conducteurMapper
    ) {
        this.conducteurRepository = conducteurRepository;
        this.conducteurMapper = conducteurMapper;
    }

    @Override
    public ConducteurResponse create(ConducteurRequest request) {
        Conducteur conducteur = conducteurMapper.toEntity(request);
        return conducteurMapper.toResponse(conducteurRepository.save(conducteur));
    }

    @Override
    public List<ConducteurResponse> findAll() {
        return conducteurRepository.findAll().stream()
                .map(conducteurMapper::toResponse)
                .toList();
    }

    @Override
    public ConducteurResponse findById(Long id) {
        return conducteurMapper.toResponse(getConducteurOrThrow(id));
    }

    @Override
    public ConducteurResponse update(Long id, ConducteurRequest request) {
        Conducteur conducteur = getConducteurOrThrow(id);
        Conducteur updatedConducteur = conducteurMapper.toEntity(request);

        conducteur.setNom(updatedConducteur.getNom());
        conducteur.setPrenom(updatedConducteur.getPrenom());
        conducteur.setEmail(updatedConducteur.getEmail());
        conducteur.setTelephone(updatedConducteur.getTelephone());
        conducteur.setNumeroPermis(updatedConducteur.getNumeroPermis());

        return conducteurMapper.toResponse(conducteurRepository.save(conducteur));
    }

    @Override
    public void delete(Long id) {
        conducteurRepository.delete(getConducteurOrThrow(id));
    }

    private Conducteur getConducteurOrThrow(Long id) {
        return conducteurRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Conducteur introuvable pour l'identifiant : " + id
                ));
    }
}
