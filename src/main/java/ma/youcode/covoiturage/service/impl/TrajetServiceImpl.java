package ma.youcode.covoiturage.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import ma.youcode.covoiturage.dto.pointarret.PointArretRequest;
import ma.youcode.covoiturage.dto.trajet.TrajetRequest;
import ma.youcode.covoiturage.dto.trajet.TrajetResponse;
import ma.youcode.covoiturage.entity.Conducteur;
import ma.youcode.covoiturage.entity.PointArret;
import ma.youcode.covoiturage.entity.Reservation;
import ma.youcode.covoiturage.entity.StatutReservation;
import ma.youcode.covoiturage.entity.StatutTrajet;
import ma.youcode.covoiturage.entity.Trajet;
import ma.youcode.covoiturage.entity.Vehicule;
import ma.youcode.covoiturage.mapper.PointArretMapper;
import ma.youcode.covoiturage.mapper.TrajetMapper;
import ma.youcode.covoiturage.repository.ConducteurRepository;
import ma.youcode.covoiturage.repository.PointArretRepository;
import ma.youcode.covoiturage.repository.ReservationRepository;
import ma.youcode.covoiturage.repository.TrajetRepository;
import ma.youcode.covoiturage.repository.VehiculeRepository;
import ma.youcode.covoiturage.service.TrajetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrajetServiceImpl implements TrajetService {

    private final TrajetRepository trajetRepository;
    private final ConducteurRepository conducteurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final PointArretRepository pointArretRepository;
    private final ReservationRepository reservationRepository;
    private final TrajetMapper trajetMapper;
    private final PointArretMapper pointArretMapper;

    public TrajetServiceImpl(
            TrajetRepository trajetRepository,
            ConducteurRepository conducteurRepository,
            VehiculeRepository vehiculeRepository,
            PointArretRepository pointArretRepository,
            ReservationRepository reservationRepository,
            TrajetMapper trajetMapper,
            PointArretMapper pointArretMapper
    ) {
        this.trajetRepository = trajetRepository;
        this.conducteurRepository = conducteurRepository;
        this.vehiculeRepository = vehiculeRepository;
        this.pointArretRepository = pointArretRepository;
        this.reservationRepository = reservationRepository;
        this.trajetMapper = trajetMapper;
        this.pointArretMapper = pointArretMapper;
    }

    @Override
    @Transactional
    public TrajetResponse create(TrajetRequest request) {
        TrajetAssociations associations = validateAndResolve(request);
        Trajet trajet = trajetMapper.toEntity(request);
        trajet.setConducteur(associations.conducteur());
        trajet.setVehicule(associations.vehicule());
        trajet.setStatut(StatutTrajet.PUBLIE);

        Trajet savedTrajet = trajetRepository.save(trajet);
        List<PointArret> pointsArret = mapStops(request.pointsArret(), savedTrajet);
        List<PointArret> savedPointsArret = pointArretRepository.saveAll(pointsArret);

        return trajetMapper.toResponse(savedTrajet, savedPointsArret);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrajetResponse> findAll() {
        Map<Long, List<PointArret>> pointsByTrajet = pointArretRepository.findAll().stream()
                .collect(Collectors.groupingBy(point -> point.getTrajet().getId()));

        return trajetRepository.findAll().stream()
                .map(trajet -> trajetMapper.toResponse(
                        trajet,
                        pointsByTrajet.getOrDefault(trajet.getId(), List.of())
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrajetResponse> search(String depart, String destination, LocalDate date,
            int places, BigDecimal prixMax, Pageable pageable) {
        if (places < 1) {
            throw new IllegalArgumentException("Le nombre de places doit etre au moins 1.");
        }
        if (prixMax != null && prixMax.signum() < 0) {
            throw new IllegalArgumentException("Le prix maximum doit etre positif ou nul.");
        }
        if (depart == null || depart.isBlank() || destination == null || destination.isBlank() || date == null) {
            throw new IllegalArgumentException("Le depart, la destination et la date sont obligatoires.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime debut = date.atStartOfDay().isAfter(now) ? date.atStartOfDay() : now;
        Page<Trajet> trajets = trajetRepository.search(depart.trim(), destination.trim(),
                debut, date.plusDays(1).atStartOfDay(), places, prixMax, pageable);
        if (trajets.isEmpty()) {
            return trajets.map(trajet -> trajetMapper.toResponse(trajet, List.of()));
        }
        List<Long> trajetIds = trajets.getContent().stream().map(Trajet::getId).toList();
        Map<Long, List<PointArret>> pointsByTrajet = pointArretRepository.findAllByTrajetIdIn(trajetIds).stream()
                .collect(Collectors.groupingBy(point -> point.getTrajet().getId()));
        return trajets.map(trajet -> trajetMapper.toResponse(
                trajet, pointsByTrajet.getOrDefault(trajet.getId(), List.of())));
    }

    @Override
    @Transactional(readOnly = true)
    public TrajetResponse findById(Long id) {
        Trajet trajet = getTrajetOrThrow(id);
        return trajetMapper.toResponse(trajet, getPointsForTrajet(id));
    }

    @Override
    @Transactional
    public TrajetResponse update(Long id, TrajetRequest request) {
        TrajetAssociations associations = validateAndResolve(request);
        Trajet trajet = getActiveTrajetForUpdate(id);
        long confirmed = reservationRepository.sumNombrePlacesByTrajetIdAndStatut(id, StatutReservation.CONFIRMEE);
        if (request.placesProposees() < confirmed) {
            throw new IllegalStateException("Des places sont déjà réservées au-delà de cette capacité.");
        }
        List<PointArret> existingPoints = getPointsForTrajet(id);
        boolean hasReservations = hasReservationsForTrajet(id);
        if (hasReservations && !stopsMatch(request.pointsArret(), existingPoints)) {
            throw new IllegalStateException(
                    "Les points d'arrêt d'un trajet réservé ne peuvent pas être remplacés."
            );
        }

        trajet.setDateDepart(request.dateDepart());
        trajet.setPlacesProposees(request.placesProposees());
        trajet.setStatut(confirmed == request.placesProposees() ? StatutTrajet.COMPLET : StatutTrajet.PUBLIE);
        trajet.setConducteur(associations.conducteur());
        trajet.setVehicule(associations.vehicule());
        Trajet savedTrajet = trajetRepository.save(trajet);

        List<PointArret> updatedPoints = existingPoints;
        if (!hasReservations && !stopsMatch(request.pointsArret(), existingPoints)) {
            pointArretRepository.deleteAll(existingPoints);
            pointArretRepository.flush();
            updatedPoints = pointArretRepository.saveAll(mapStops(request.pointsArret(), savedTrajet));
        }

        return trajetMapper.toResponse(savedTrajet, updatedPoints);
    }

    @Override
    @Transactional
    public TrajetResponse complete(Long id) {
        Trajet trajet = getActiveTrajetForUpdate(id);
        if (!trajet.getDateDepart().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Le trajet ne peut etre termine qu'apres son depart.");
        }
        trajet.setStatut(StatutTrajet.TERMINE);
        return trajetMapper.toResponse(trajetRepository.save(trajet), getPointsForTrajet(id));
    }

    @Override
    @Transactional
    public TrajetResponse cancel(Long id) {
        Trajet trajet = getActiveTrajetForUpdate(id);
        List<Reservation> confirmedReservations = reservationRepository.findAll().stream()
                .filter(reservation -> id.equals(reservation.getTrajet().getId()))
                .filter(reservation -> reservation.getStatut() == StatutReservation.CONFIRMEE)
                .toList();
        confirmedReservations.forEach(reservation -> reservation.setStatut(StatutReservation.ANNULEE));
        reservationRepository.saveAll(confirmedReservations);
        trajet.setStatut(StatutTrajet.ANNULE);
        return trajetMapper.toResponse(trajetRepository.save(trajet), getPointsForTrajet(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Trajet trajet = trajetRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NoSuchElementException("Trajet introuvable : " + id));
        if (hasReservationsForTrajet(id)) {
            throw new IllegalStateException(
                    "Le trajet ne peut pas être supprimé car des réservations y sont associées : " + id
            );
        }

        pointArretRepository.deleteAll(getPointsForTrajet(id));
        trajetRepository.delete(trajet);
    }

    private TrajetAssociations validateAndResolve(TrajetRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La demande de trajet est obligatoire.");
        }

        Conducteur conducteur = conducteurRepository.findById(request.conducteurId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Conducteur introuvable pour l'identifiant : " + request.conducteurId()
                ));
        Vehicule vehicule = vehiculeRepository.findByIdForUpdate(request.vehiculeId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Véhicule introuvable pour l'identifiant : " + request.vehiculeId()
                ));

        validateRequest(request, conducteur, vehicule);
        return new TrajetAssociations(conducteur, vehicule);
    }

    private void validateRequest(TrajetRequest request, Conducteur conducteur, Vehicule vehicule) {
        if (vehicule.getConducteur() == null
                || !Objects.equals(vehicule.getConducteur().getId(), conducteur.getId())) {
            throw new IllegalArgumentException(
                    "Le véhicule " + vehicule.getId() + " n'appartient pas au conducteur " + conducteur.getId()
            );
        }

        if (request.placesProposees() == null
                || request.placesProposees() < 1
                || vehicule.getCapacite() == null
                || request.placesProposees() > vehicule.getCapacite()) {
            throw new IllegalArgumentException(
                    "Le nombre de places proposées doit être compris entre 1 et la capacité du véhicule."
            );
        }

        if (request.dateDepart() == null || !request.dateDepart().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La date de départ doit être dans le futur.");
        }

        ValidationArrets.validate(request.dateDepart(), request.pointsArret());
    }

    private List<PointArret> mapStops(List<PointArretRequest> requests, Trajet trajet) {
        return requests.stream()
                .map(request -> {
                    PointArret pointArret = pointArretMapper.toEntity(request);
                    pointArret.setTrajet(trajet);
                    return pointArret;
                })
                .toList();
    }

    private List<PointArret> getPointsForTrajet(Long trajetId) {
        return pointArretRepository.findAllByTrajetIdIn(List.of(trajetId));
    }

    private boolean hasReservationsForTrajet(Long trajetId) {
        return reservationRepository.findAll().stream()
                .map(Reservation::getTrajet)
                .filter(Objects::nonNull)
                .anyMatch(reservationTrajet -> trajetId.equals(reservationTrajet.getId()));
    }

    private boolean stopsMatch(List<PointArretRequest> requests, List<PointArret> existingPoints) {
        List<PointArret> sortedExistingPoints = existingPoints.stream()
                .sorted(Comparator.comparing(PointArret::getOrdre))
                .toList();

        if (requests.size() != sortedExistingPoints.size()) {
            return false;
        }

        for (int index = 0; index < requests.size(); index++) {
            PointArretRequest request = requests.get(index);
            PointArret existingPoint = sortedExistingPoints.get(index);
            if (!Objects.equals(request.ville(), existingPoint.getVille())
                    || !Objects.equals(request.ordre(), existingPoint.getOrdre())
                    || !Objects.equals(request.heurePassage(), existingPoint.getHeurePassage())
                    || request.prixDepuisDepart().compareTo(existingPoint.getPrixDepuisDepart()) != 0) {
                return false;
            }
        }
        return true;
    }

    private Trajet getActiveTrajetForUpdate(Long id) {
        // Use the same lock as reservation creation and cancellation.
        Trajet trajet = trajetRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NoSuchElementException("Trajet introuvable : " + id));
        if (trajet.getStatut() != StatutTrajet.PUBLIE && trajet.getStatut() != StatutTrajet.COMPLET) {
            throw new IllegalStateException("Un trajet termine ou annule ne peut plus changer de statut.");
        }
        return trajet;
    }

    private Trajet getTrajetOrThrow(Long id) {
        return trajetRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Trajet introuvable pour l'identifiant : " + id
                ));
    }

    private record TrajetAssociations(Conducteur conducteur, Vehicule vehicule) {
    }
}
