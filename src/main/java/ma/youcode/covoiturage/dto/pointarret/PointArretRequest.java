package ma.youcode.covoiturage.dto.pointarret;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PointArretRequest(
        @NotBlank String ville,
        @NotNull @Min(1) Integer ordre,
        @NotNull LocalDateTime heurePassage,
        @NotNull BigDecimal prixDepuisDepart
) {
}
