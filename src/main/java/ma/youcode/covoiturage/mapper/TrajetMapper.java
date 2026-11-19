package ma.youcode.covoiturage.mapper;

import java.util.List;
import ma.youcode.covoiturage.dto.pointarret.PointArretResponse;
import ma.youcode.covoiturage.dto.trajet.TrajetRequest;
import ma.youcode.covoiturage.dto.trajet.TrajetResponse;
import ma.youcode.covoiturage.entity.PointArret;
import ma.youcode.covoiturage.entity.Trajet;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        implementationPackage = "ma.youcode.covoiturage.entity",
        uses = PointArretMapper.class
)
public interface TrajetMapper {

    @BeanMapping(ignoreUnmappedSourceProperties = {"conducteurId", "vehiculeId", "pointsArret"})
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "conducteur", ignore = true)
    @Mapping(target = "vehicule", ignore = true)
    Trajet toEntity(TrajetRequest request);

    @Mapping(target = "id", source = "trajet.id")
    @Mapping(target = "dateDepart", source = "trajet.dateDepart")
    @Mapping(target = "placesProposees", source = "trajet.placesProposees")
    @Mapping(target = "statut", source = "trajet.statut")
    @Mapping(target = "conducteurId", source = "trajet.conducteur.id")
    @Mapping(target = "vehiculeId", source = "trajet.vehicule.id")
    @Mapping(target = "pointsArret", source = "pointsArret")
    TrajetResponse toResponse(Trajet trajet, List<PointArret> pointsArret);
}
