package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.vehicule.VehiculeRequest;
import ma.youcode.covoiturage.dto.vehicule.VehiculeResponse;
import ma.youcode.covoiturage.entity.Vehicule;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", implementationPackage = "ma.youcode.covoiturage.entity")
public interface VehiculeMapper {

    @BeanMapping(ignoreUnmappedSourceProperties = {"conducteurId"})
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conducteur", ignore = true)
    Vehicule toEntity(VehiculeRequest request);

    @Mapping(target = "conducteurId", source = "conducteur.id")
    VehiculeResponse toResponse(Vehicule vehicule);
}
