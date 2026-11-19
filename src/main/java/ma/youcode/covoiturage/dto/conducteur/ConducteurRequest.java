package ma.youcode.covoiturage.dto.conducteur;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ConducteurRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotBlank @Email String email,
        @NotBlank String telephone,
        @NotBlank String numeroPermis
) {
}
