package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.conducteur.ConducteurRequest;
import ma.youcode.covoiturage.dto.conducteur.ConducteurResponse;
import ma.youcode.covoiturage.entity.Conducteur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", implementationPackage = "ma.youcode.covoiturage.entity")
public interface ConducteurMapper {

    @Mapping(target = "id", ignore = true)
    Conducteur toEntity(ConducteurRequest request);

    ConducteurResponse toResponse(Conducteur conducteur);
}
