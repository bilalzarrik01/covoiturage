package ma.youcode.covoiturage.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import ma.youcode.covoiturage.dto.reservation.ReservationRequest;
import ma.youcode.covoiturage.dto.reservation.ReservationResponse;
import ma.youcode.covoiturage.entity.Passager;
import ma.youcode.covoiturage.entity.PointArret;
import ma.youcode.covoiturage.entity.Reservation;
import ma.youcode.covoiturage.entity.StatutReservation;
import ma.youcode.covoiturage.entity.StatutTrajet;
import ma.youcode.covoiturage.entity.Trajet;
import ma.youcode.covoiturage.exception.ConflictException;
import ma.youcode.covoiturage.mapper.ReservationMapper;
import ma.youcode.covoiturage.repository.PassagerRepository;
import ma.youcode.covoiturage.repository.PointArretRepository;
import ma.youcode.covoiturage.repository.ReservationRepository;
import ma.youcode.covoiturage.repository.TrajetRepository;
import ma.youcode.covoiturage.service.ReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final TrajetRepository trajetRepository;
    private final PassagerRepository passagerRepository;
    private final PointArretRepository pointArretRepository;
    private final ReservationMapper reservationMapper;

    public ReservationServiceImpl(ReservationRepository reservationRepository,
            TrajetRepository trajetRepository, PassagerRepository passagerRepository,
            PointArretRepository pointArretRepository, ReservationMapper reservationMapper) {
        this.reservationRepository = reservationRepository;
        this.trajetRepository = trajetRepository;
        this.passagerRepository = passagerRepository;
        this.pointArretRepository = pointArretRepository;
        this.reservationMapper = reservationMapper;
    }

    @Override
    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        if (request == null || request.passagerId() == null || request.trajetId() == null
                || request.pointMonteeId() == null || request.pointDescenteId() == null
                || request.nombrePlaces() == null || request.nombrePlaces() < 1) {
            throw new IllegalArgumentException("La reservation doit etre complete avec au moins une place.");
        }

        // All booking and cancellation capacity checks are serialized by the trip row lock.
        Trajet trajet = lockTrajet(request.trajetId());
        LocalDateTime now = LocalDateTime.now();
        if (trajet.getStatut() != StatutTrajet.PUBLIE || !trajet.getDateDepart().isAfter(now)) {
            throw new IllegalStateException("Le trajet doit etre publie et son depart dans le futur.");
        }
        Passager passager = passagerRepository.findById(request.passagerId())
                .orElseThrow(() -> new NoSuchElementException("Passager introuvable : " + request.passagerId()));
        PointArret montee = getPoint(request.pointMonteeId());
        PointArret descente = getPoint(request.pointDescenteId());
        if (!Objects.equals(montee.getTrajet().getId(), trajet.getId())
                || !Objects.equals(descente.getTrajet().getId(), trajet.getId())
                || montee.getOrdre() >= descente.getOrdre()) {
            throw new IllegalArgumentException("Les points doivent appartenir au trajet et etre dans le bon ordre.");
        }
        if (reservationRepository.existsByPassagerIdAndTrajetIdAndStatut(
                request.passagerId(), trajet.getId(), StatutReservation.CONFIRMEE)) {
            throw new ConflictException("Le passager a deja une reservation confirmee pour ce trajet.");
        }
        long available = trajet.getPlacesProposees() - confirmedSeats(trajet.getId());
        if (request.nombrePlaces() > available) {
            throw new ConflictException("Le trajet ne dispose pas de suffisamment de places.");
        }

        BigDecimal prixSegment = descente.getPrixDepuisDepart().subtract(montee.getPrixDepuisDepart());
        if (prixSegment.signum() <= 0) {
            throw new IllegalStateException("Le tarif du segment doit être positif.");
        }
        Reservation reservation = reservationMapper.toEntity(request);
        reservation.setPassager(passager);
        reservation.setTrajet(trajet);
        reservation.setPointMontee(montee);
        reservation.setPointDescente(descente);
        reservation.setMontantTotal(prixSegment.multiply(BigDecimal.valueOf(request.nombrePlaces())));
        reservation.setStatut(StatutReservation.CONFIRMEE);
        reservation.setDateReservation(now);
        Reservation saved = reservationRepository.save(reservation);
        if (available == request.nombrePlaces()) {
            trajet.setStatut(StatutTrajet.COMPLET);
        }
        return reservationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ReservationResponse cancel(Long id) {
        // Read only the trip ID before locking, so reservation state is loaded after acquiring the lock.
        Long trajetId = reservationRepository.findTrajetIdById(id)
                .orElseThrow(() -> new NoSuchElementException("Reservation introuvable : " + id));
        Trajet trajet = lockTrajet(trajetId);
        Reservation reservation = getReservation(id);
        if (reservation.getStatut() != StatutReservation.CONFIRMEE) {
            throw new IllegalStateException("La reservation est deja annulee.");
        }
        if (trajet.getDateDepart().isBefore(LocalDateTime.now().plusHours(24))) {
            throw new IllegalStateException("L'annulation exige au moins 24 heures avant le depart.");
        }
        long remainingConfirmedSeats = confirmedSeats(trajetId) - reservation.getNombrePlaces();
        reservation.setStatut(StatutReservation.ANNULEE);
        Reservation saved = reservationRepository.save(reservation);
        if (trajet.getStatut() == StatutTrajet.COMPLET && remainingConfirmedSeats < trajet.getPlacesProposees()) {
            trajet.setStatut(StatutTrajet.PUBLIE);
        }
        return reservationMapper.toResponse(saved);
    }

    @Override
    public List<ReservationResponse> findAll() {
        return reservationRepository.findAll().stream().map(reservationMapper::toResponse).toList();
    }

    @Override
    public ReservationResponse findById(Long id) {
        return reservationMapper.toResponse(getReservation(id));
    }

    @Override
    public List<ReservationResponse> findByPassagerId(Long passagerId) {
        return reservationRepository.findAllByPassagerId(passagerId).stream()
                .map(reservationMapper::toResponse).toList();
    }

    private Trajet lockTrajet(Long id) {
        return trajetRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NoSuchElementException("Trajet introuvable : " + id));
    }

    private PointArret getPoint(Long id) {
        return pointArretRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Point d'arret introuvable : " + id));
    }

    private Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reservation introuvable : " + id));
    }

    private long confirmedSeats(Long trajetId) {
        return reservationRepository.sumNombrePlacesByTrajetIdAndStatut(trajetId, StatutReservation.CONFIRMEE);
    }
}
