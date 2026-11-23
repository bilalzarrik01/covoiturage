package ma.youcode.covoiturage.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ma.youcode.covoiturage.entity.Vehicule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehiculeRepository extends JpaRepository<Vehicule, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicule v where v.id = :id")
    Optional<Vehicule> findByIdForUpdate(@Param("id") Long id);
}
