package ma.youcode.covoiturage.dto.trajet;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import ma.youcode.covoiturage.dto.pointarret.PointArretRequest;

public record TrajetRequest(
        @NotNull LocalDateTime dateDepart,
        @NotNull @Min(1) Integer placesProposees,
        @NotNull Long conducteurId,
        @NotNull Long vehiculeId,
        @NotEmpty @Valid List<PointArretRequest> pointsArret
) {
}
