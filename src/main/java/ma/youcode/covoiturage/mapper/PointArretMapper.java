package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.pointarret.PointArretRequest;
import ma.youcode.covoiturage.dto.pointarret.PointArretResponse;
import ma.youcode.covoiturage.entity.PointArret;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", implementationPackage = "ma.youcode.covoiturage.entity")
public interface PointArretMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "trajet", ignore = true)
    PointArret toEntity(PointArretRequest request);

    PointArretRequest toRequest(PointArret pointArret);

    @BeanMapping(ignoreUnmappedSourceProperties = {"trajet"})
    PointArretResponse toResponse(PointArret pointArret);
}
