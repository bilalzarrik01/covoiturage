package ma.youcode.covoiturage.dto.reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ma.youcode.covoiturage.entity.StatutReservation;

public record ReservationResponse(
        Long id,
        Integer nombrePlaces,
        BigDecimal montantTotal,
        LocalDateTime dateReservation,
        StatutReservation statut,
        Long passagerId,
        Long trajetId,
        Long pointMonteeId,
        Long pointDescenteId
) {
}
