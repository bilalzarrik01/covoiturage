package ma.youcode.covoiturage.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import ma.youcode.covoiturage.dto.pointarret.PointArretRequest;
import ma.youcode.covoiturage.dto.pointarret.PointArretResponse;
import ma.youcode.covoiturage.entity.PointArret;
import ma.youcode.covoiturage.entity.StatutReservation;
import ma.youcode.covoiturage.entity.StatutTrajet;
import ma.youcode.covoiturage.entity.Trajet;
import ma.youcode.covoiturage.mapper.PointArretMapper;
import ma.youcode.covoiturage.repository.PointArretRepository;
import ma.youcode.covoiturage.repository.ReservationRepository;
import ma.youcode.covoiturage.repository.TrajetRepository;
import ma.youcode.covoiturage.service.PointArretService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointArretServiceImpl implements PointArretService {

    private final PointArretRepository pointArretRepository;
    private final TrajetRepository trajetRepository;
    private final PointArretMapper pointArretMapper;

    private final ReservationRepository reservationRepository;

    public PointArretServiceImpl(
            PointArretRepository pointArretRepository,
            TrajetRepository trajetRepository,
            PointArretMapper pointArretMapper,
            ReservationRepository reservationRepository
    ) {
        this.pointArretRepository = pointArretRepository;
        this.trajetRepository = trajetRepository;
        this.pointArretMapper = pointArretMapper;
        this.reservationRepository = reservationRepository;
    }

    @Override
    @Transactional
    public PointArretResponse create(Long trajetId, PointArretRequest request) {
        Trajet trajet = lockEditableTrip(trajetId);
        validateChange(trajet, null, request);
        PointArret pointArret = pointArretMapper.toEntity(request);
        pointArret.setTrajet(trajet);
        return pointArretMapper.toResponse(pointArretRepository.save(pointArret));
    }

    @Override
    public List<PointArretResponse> findAll() {
        return pointArretRepository.findAll().stream()
                .map(pointArretMapper::toResponse)
                .toList();
    }

    @Override
    public PointArretResponse findById(Long id) {
        return pointArretMapper.toResponse(getPointArretOrThrow(id));
    }

    @Override
    public List<PointArretResponse> findByTrajetId(Long trajetId) {
        return pointArretRepository.findAllByTrajetIdIn(List.of(trajetId)).stream()
                .map(pointArretMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PointArretResponse update(Long id, PointArretRequest request) {
        Trajet trajet = lockTripForStop(id);
        PointArret pointArret = getPointArretOrThrow(id);
        validateChange(trajet, id, request);
        PointArret updatedPointArret = pointArretMapper.toEntity(request);

        pointArret.setVille(updatedPointArret.getVille());
        pointArret.setOrdre(updatedPointArret.getOrdre());
        pointArret.setHeurePassage(updatedPointArret.getHeurePassage());
        pointArret.setPrixDepuisDepart(updatedPointArret.getPrixDepuisDepart());
        pointArret.setTrajet(trajet);

        return pointArretMapper.toResponse(pointArretRepository.save(pointArret));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Trajet trajet = lockTripForStop(id);
        PointArret point = getPointArretOrThrow(id);
        validateChange(trajet, id, null);
        pointArretRepository.delete(point);
    }

    private PointArret getPointArretOrThrow(Long id) {
        return pointArretRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Point d'arrêt introuvable pour l'identifiant : " + id
                ));
    }

    private Trajet lockTripForStop(Long id) {
        Long trajetId = pointArretRepository.findTrajetIdById(id)
                .orElseThrow(() -> new NoSuchElementException("Point d'arrêt introuvable : " + id));
        return lockEditableTrip(trajetId);
    }

    private Trajet lockEditableTrip(Long id) {
        Trajet trajet = trajetRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NoSuchElementException("Trajet introuvable : " + id));
        if (trajet.getStatut() == StatutTrajet.TERMINE || trajet.getStatut() == StatutTrajet.ANNULE) {
            throw new IllegalStateException("Les arrêts d'un trajet terminé ou annulé ne sont plus modifiables.");
        }
        return trajet;
    }

    private void validateChange(Trajet trajet, Long replacedId, PointArretRequest replacement) {
        if (replacedId != null && reservationRepository.usesStop(replacedId, StatutReservation.CONFIRMEE)) {
            throw new IllegalStateException("Cet arrêt est utilisé par une réservation confirmée.");
        }
        List<PointArretRequest> stops = new ArrayList<>();
        for (PointArret stop : pointArretRepository.findAllByTrajetIdIn(List.of(trajet.getId()))) {
            if (!stop.getId().equals(replacedId)) {
                stops.add(pointArretMapper.toRequest(stop));
            }
        }
        if (replacement != null) {
            stops.add(replacement);
        }
        stops.sort(Comparator.comparing(PointArretRequest::ordre));
        ValidationArrets.validate(trajet.getDateDepart(), stops);
    }
}
