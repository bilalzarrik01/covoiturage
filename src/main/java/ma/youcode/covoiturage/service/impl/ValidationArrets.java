package ma.youcode.covoiturage.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import ma.youcode.covoiturage.dto.pointarret.PointArretRequest;

final class ValidationArrets {
    private ValidationArrets() {
    }

    static void validate(LocalDateTime dateDepart, List<PointArretRequest> pointsArret) {
        if (pointsArret == null || pointsArret.size() < 2) {
            throw new IllegalArgumentException("Un trajet doit comporter au moins deux points d'arrêt.");
        }

        Set<String> villes = new HashSet<>();
        Integer ordrePrecedent = null;
        LocalDateTime heurePrecedente = null;
        BigDecimal prixPrecedent = null;

        for (int index = 0; index < pointsArret.size(); index++) {
            PointArretRequest point = pointsArret.get(index);
            if (point == null
                    || point.ville() == null
                    || point.ville().isBlank()
                    || point.ordre() == null || point.ordre() < 1
                    || point.heurePassage() == null
                    || point.prixDepuisDepart() == null) {
                throw new IllegalArgumentException("Chaque point d'arrêt doit être complet.");
            }

            if (ordrePrecedent != null && point.ordre() <= ordrePrecedent) {
                throw new IllegalArgumentException("Les ordres des points d'arrêt doivent être strictement croissants.");
            }

            String villeNormalisee = point.ville().trim().toLowerCase(Locale.ROOT);
            if (!villes.add(villeNormalisee)) {
                throw new IllegalArgumentException("Les villes des points d'arrêt doivent être uniques.");
            }

            if (point.heurePassage().isBefore(dateDepart)) {
                throw new IllegalArgumentException("Un arrêt ne peut pas précéder le départ du trajet.");
            }
            if (point.prixDepuisDepart().stripTrailingZeros().scale() > 2) {
                throw new IllegalArgumentException("Le prix d'un arrêt doit être exprimé au centime.");
            }
            if (heurePrecedente != null && !point.heurePassage().isAfter(heurePrecedente)) {
                throw new IllegalArgumentException("Les heures de passage doivent être strictement croissantes.");
            }

            if (index == 0 && point.prixDepuisDepart().compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("Le prix du premier point d'arrêt doit être égal à zéro.");
            }

            if (prixPrecedent != null && point.prixDepuisDepart().compareTo(prixPrecedent) <= 0) {
                throw new IllegalArgumentException("Les prix cumulés doivent être strictement croissants.");
            }

            ordrePrecedent = point.ordre();
            heurePrecedente = point.heurePassage();
            prixPrecedent = point.prixDepuisDepart();
        }
    }
}
