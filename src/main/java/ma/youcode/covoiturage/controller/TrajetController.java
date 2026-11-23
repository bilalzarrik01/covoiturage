package ma.youcode.covoiturage.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ma.youcode.covoiturage.dto.trajet.TrajetRequest;
import ma.youcode.covoiturage.dto.trajet.TrajetResponse;
import ma.youcode.covoiturage.service.TrajetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trajets")
public class TrajetController {

    private final TrajetService trajetService;

    public TrajetController(TrajetService trajetService) {
        this.trajetService = trajetService;
    }

    @PostMapping
    public ResponseEntity<TrajetResponse> create(@Valid @RequestBody TrajetRequest request) {
        TrajetResponse response = trajetService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TrajetResponse>> findAll() {
        return ResponseEntity.ok(trajetService.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<TrajetResponse>> search(
            @RequestParam("depart") @NotBlank String depart,
            @RequestParam("destination") @NotBlank String destination,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "places", defaultValue = "1") @Min(1) int places,
            @RequestParam(name = "prixMax", required = false) @DecimalMin("0") BigDecimal prixMax,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) int size
    ) {
        return ResponseEntity.ok(trajetService.search(depart, destination, date, places, prixMax,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "dateDepart"))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrajetResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(trajetService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrajetResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TrajetRequest request
    ) {
        return ResponseEntity.ok(trajetService.update(id, request));
    }

    @PatchMapping("/{id}/terminer")
    public ResponseEntity<TrajetResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(trajetService.complete(id));
    }

    @PatchMapping("/{id}/annuler")
    public ResponseEntity<TrajetResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(trajetService.cancel(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        trajetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
