package ma.youcode.covoiturage.dto.vehicule;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehiculeRequest(
        @NotBlank String marque,
        @NotBlank String modele,
        @NotBlank String immatriculation,
        @NotBlank String couleur,
        @NotNull @Min(1) @Max(8) Integer capacite,
        @NotNull Long conducteurId
) {
}
