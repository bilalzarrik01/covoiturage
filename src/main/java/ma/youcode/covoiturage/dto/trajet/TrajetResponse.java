package ma.youcode.covoiturage.dto.trajet;

import java.time.LocalDateTime;
import java.util.List;
import ma.youcode.covoiturage.dto.pointarret.PointArretResponse;
import ma.youcode.covoiturage.entity.StatutTrajet;

public record TrajetResponse(
        Long id,
        LocalDateTime dateDepart,
        Integer placesProposees,
        StatutTrajet statut,
        Long conducteurId,
        Long vehiculeId,
        List<PointArretResponse> pointsArret
) {
}
