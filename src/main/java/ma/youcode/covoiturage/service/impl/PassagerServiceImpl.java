package ma.youcode.covoiturage.service.impl;

import java.util.List;
import java.util.NoSuchElementException;
import ma.youcode.covoiturage.dto.passager.PassagerRequest;
import ma.youcode.covoiturage.dto.passager.PassagerResponse;
import ma.youcode.covoiturage.entity.Passager;
import ma.youcode.covoiturage.mapper.PassagerMapper;
import ma.youcode.covoiturage.repository.PassagerRepository;
import ma.youcode.covoiturage.service.PassagerService;
import org.springframework.stereotype.Service;

@Service
public class PassagerServiceImpl implements PassagerService {

    private final PassagerRepository passagerRepository;
    private final PassagerMapper passagerMapper;

    public PassagerServiceImpl(
            PassagerRepository passagerRepository,
            PassagerMapper passagerMapper
    ) {
        this.passagerRepository = passagerRepository;
        this.passagerMapper = passagerMapper;
    }

    @Override
    public PassagerResponse create(PassagerRequest request) {
        Passager passager = passagerMapper.toEntity(request);
        return passagerMapper.toResponse(passagerRepository.save(passager));
    }

    @Override
    public List<PassagerResponse> findAll() {
        return passagerRepository.findAll().stream()
                .map(passagerMapper::toResponse)
                .toList();
    }

    @Override
    public PassagerResponse findById(Long id) {
        return passagerMapper.toResponse(getPassagerOrThrow(id));
    }

    @Override
    public PassagerResponse update(Long id, PassagerRequest request) {
        Passager passager = getPassagerOrThrow(id);
        Passager updatedPassager = passagerMapper.toEntity(request);

        passager.setNom(updatedPassager.getNom());
        passager.setPrenom(updatedPassager.getPrenom());
        passager.setEmail(updatedPassager.getEmail());
        passager.setTelephone(updatedPassager.getTelephone());

        return passagerMapper.toResponse(passagerRepository.save(passager));
    }

    @Override
    public void delete(Long id) {
        passagerRepository.delete(getPassagerOrThrow(id));
    }

    private Passager getPassagerOrThrow(Long id) {
        return passagerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Passager introuvable pour l'identifiant : " + id
                ));
    }
}
