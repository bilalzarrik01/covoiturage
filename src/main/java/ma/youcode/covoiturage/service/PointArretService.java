package ma.youcode.covoiturage.service;

import java.util.List;
import ma.youcode.covoiturage.dto.pointarret.PointArretRequest;
import ma.youcode.covoiturage.dto.pointarret.PointArretResponse;

public interface PointArretService {

    PointArretResponse create(Long trajetId, PointArretRequest request);

    List<PointArretResponse> findAll();

    PointArretResponse findById(Long id);

    List<PointArretResponse> findByTrajetId(Long trajetId);

    PointArretResponse update(Long id, PointArretRequest request);

    void delete(Long id);
}
