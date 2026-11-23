package ma.youcode.covoiturage.controller;

import jakarta.validation.Valid;
import java.util.List;
import ma.youcode.covoiturage.dto.vehicule.VehiculeRequest;
import ma.youcode.covoiturage.dto.vehicule.VehiculeResponse;
import ma.youcode.covoiturage.service.VehiculeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicules")
public class VehiculeController {

    private final VehiculeService vehiculeService;

    public VehiculeController(VehiculeService vehiculeService) {
        this.vehiculeService = vehiculeService;
    }

    @PostMapping
    public ResponseEntity<VehiculeResponse> create(@Valid @RequestBody VehiculeRequest request) {
        VehiculeResponse response = vehiculeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<VehiculeResponse>> findAll() {
        return ResponseEntity.ok(vehiculeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehiculeResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(vehiculeService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehiculeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody VehiculeRequest request
    ) {
        return ResponseEntity.ok(vehiculeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehiculeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
