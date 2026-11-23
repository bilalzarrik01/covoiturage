package ma.youcode.covoiturage.repository;

import ma.youcode.covoiturage.entity.Reservation;
import ma.youcode.covoiturage.entity.StatutReservation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            select count(r) > 0 from Reservation r
            where r.statut = :statut and (r.pointMontee.id = :id or r.pointDescente.id = :id)
            """)
    boolean usesStop(@Param("id") Long id, @Param("statut") StatutReservation statut);

    List<Reservation> findAllByPassagerId(Long passagerId);

    boolean existsByPassagerIdAndTrajetIdAndStatut(Long passagerId, Long trajetId, StatutReservation statut);

    @Query("select coalesce(sum(r.nombrePlaces), 0) from Reservation r where r.trajet.id = :trajetId and r.statut = :statut")
    long sumNombrePlacesByTrajetIdAndStatut(@Param("trajetId") Long trajetId,
            @Param("statut") StatutReservation statut);

    @Query("select r.trajet.id from Reservation r where r.id = :id")
    Optional<Long> findTrajetIdById(@Param("id") Long id);
}
