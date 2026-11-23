package ma.youcode.covoiturage.controller;

import jakarta.validation.Valid;
import java.util.List;
import ma.youcode.covoiturage.dto.pointarret.PointArretRequest;
import ma.youcode.covoiturage.dto.pointarret.PointArretResponse;
import ma.youcode.covoiturage.service.PointArretService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points-arret")
public class PointArretController {

    private final PointArretService pointArretService;

    public PointArretController(PointArretService pointArretService) {
        this.pointArretService = pointArretService;
    }

    @PostMapping
    public ResponseEntity<PointArretResponse> create(
            @RequestParam Long trajetId,
            @Valid @RequestBody PointArretRequest request
    ) {
        PointArretResponse response = pointArretService.create(trajetId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PointArretResponse>> findAll() {
        return ResponseEntity.ok(pointArretService.findAll());
    }

    @GetMapping("/trajet/{trajetId}")
    public ResponseEntity<List<PointArretResponse>> findByTrajetId(@PathVariable Long trajetId) {
        return ResponseEntity.ok(pointArretService.findByTrajetId(trajetId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PointArretResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(pointArretService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PointArretResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PointArretRequest request
    ) {
        return ResponseEntity.ok(pointArretService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pointArretService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
