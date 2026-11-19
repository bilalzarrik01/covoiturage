package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.reservation.ReservationRequest;
import ma.youcode.covoiturage.dto.reservation.ReservationResponse;
import ma.youcode.covoiturage.entity.Reservation;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", implementationPackage = "ma.youcode.covoiturage.entity")
public interface ReservationMapper {

    @BeanMapping(ignoreUnmappedSourceProperties = {
        "passagerId", "trajetId", "pointMonteeId", "pointDescenteId"
    })
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "montantTotal", ignore = true)
    @Mapping(target = "dateReservation", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "passager", ignore = true)
    @Mapping(target = "trajet", ignore = true)
    @Mapping(target = "pointMontee", ignore = true)
    @Mapping(target = "pointDescente", ignore = true)
    Reservation toEntity(ReservationRequest request);

    @Mapping(target = "passagerId", source = "passager.id")
    @Mapping(target = "trajetId", source = "trajet.id")
    @Mapping(target = "pointMonteeId", source = "pointMontee.id")
    @Mapping(target = "pointDescenteId", source = "pointDescente.id")
    ReservationResponse toResponse(Reservation reservation);
}
