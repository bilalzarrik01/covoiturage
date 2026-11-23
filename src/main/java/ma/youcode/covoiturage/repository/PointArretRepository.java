package ma.youcode.covoiturage.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import ma.youcode.covoiturage.entity.PointArret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointArretRepository extends JpaRepository<PointArret, Long> {

    @Query("select p.trajet.id from PointArret p where p.id = :id")
    Optional<Long> findTrajetIdById(@Param("id") Long id);

    @Query("select p from PointArret p where p.trajet.id in :trajetIds order by p.trajet.id, p.ordre")
    List<PointArret> findAllByTrajetIdIn(@Param("trajetIds") Collection<Long> trajetIds);
}
