package ma.youcode.covoiturage.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ma.youcode.covoiturage.dto.trajet.TrajetRequest;
import ma.youcode.covoiturage.dto.trajet.TrajetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TrajetService {

    TrajetResponse create(TrajetRequest request);

    List<TrajetResponse> findAll();

    Page<TrajetResponse> search(String depart, String destination, LocalDate date,
            int places, BigDecimal prixMax, Pageable pageable);

    TrajetResponse findById(Long id);

    TrajetResponse update(Long id, TrajetRequest request);

    TrajetResponse complete(Long id);

    TrajetResponse cancel(Long id);

    void delete(Long id);
}
