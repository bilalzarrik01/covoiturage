package ma.youcode.covoiturage.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import ma.youcode.covoiturage.entity.Trajet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrajetRepository extends JpaRepository<Trajet, Long> {

    boolean existsByVehiculeIdAndPlacesProposeesGreaterThan(Long vehiculeId, Integer capacite);

    boolean existsByVehiculeIdAndConducteurIdNot(Long vehiculeId, Long conducteurId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trajet t where t.id = :id")
    Optional<Trajet> findByIdForUpdate(@Param("id") Long id);

    // EXISTS keeps each trip unique, including in the pagination count.
    String SEARCH_FILTER = """
            where t.statut = ma.youcode.covoiturage.entity.StatutTrajet.PUBLIE
              and t.dateDepart >= :debutJour and t.dateDepart < :finJour
              and exists (
                  select depart.id from PointArret depart, PointArret destination
                  where depart.trajet = t and destination.trajet = t
                    and lower(trim(depart.ville)) = lower(trim(:depart))
                    and lower(trim(destination.ville)) = lower(trim(:destination))
                    and depart.ordre < destination.ordre
                    and (:prixMax is null
                         or destination.prixDepuisDepart - depart.prixDepuisDepart <= :prixMax)
              )
              and t.placesProposees - (
                  select coalesce(sum(r.nombrePlaces), 0) from Reservation r
                  where r.trajet = t
                    and r.statut = ma.youcode.covoiturage.entity.StatutReservation.CONFIRMEE
              ) >= :places
            """;

    // Confirmed reservations consume capacity for the entire trip, irrespective of stops.
    @Query(value = "select t from Trajet t " + SEARCH_FILTER + " order by t.dateDepart asc, t.id asc",
            countQuery = "select count(t) from Trajet t " + SEARCH_FILTER)
    Page<Trajet> search(
            @Param("depart") String depart,
            @Param("destination") String destination,
            @Param("debutJour") LocalDateTime debutJour,
            @Param("finJour") LocalDateTime finJour,
            @Param("places") int places,
            @Param("prixMax") BigDecimal prixMax,
            Pageable pageable
    );
}
