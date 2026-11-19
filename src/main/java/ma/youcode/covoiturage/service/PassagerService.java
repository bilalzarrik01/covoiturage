package ma.youcode.covoiturage.service;

import java.util.List;
import ma.youcode.covoiturage.dto.passager.PassagerRequest;
import ma.youcode.covoiturage.dto.passager.PassagerResponse;

public interface PassagerService {

    PassagerResponse create(PassagerRequest request);

    List<PassagerResponse> findAll();

    PassagerResponse findById(Long id);

    PassagerResponse update(Long id, PassagerRequest request);

    void delete(Long id);
}
