package ma.youcode.covoiturage.service;

import java.util.List;
import ma.youcode.covoiturage.dto.reservation.ReservationRequest;
import ma.youcode.covoiturage.dto.reservation.ReservationResponse;

public interface ReservationService {

    ReservationResponse create(ReservationRequest request);

    ReservationResponse cancel(Long id);

    List<ReservationResponse> findAll();

    ReservationResponse findById(Long id);

    List<ReservationResponse> findByPassagerId(Long passagerId);
}
