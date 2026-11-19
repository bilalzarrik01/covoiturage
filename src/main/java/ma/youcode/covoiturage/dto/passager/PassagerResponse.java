package ma.youcode.covoiturage.dto.passager;

public record PassagerResponse(
        Long id,
        String nom,
        String prenom,
        String email,
        String telephone
) {
}
