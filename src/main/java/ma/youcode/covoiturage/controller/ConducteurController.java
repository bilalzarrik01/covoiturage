package ma.youcode.covoiturage.controller;

import jakarta.validation.Valid;
import java.util.List;
import ma.youcode.covoiturage.dto.conducteur.ConducteurRequest;
import ma.youcode.covoiturage.dto.conducteur.ConducteurResponse;
import ma.youcode.covoiturage.service.ConducteurService;
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
@RequestMapping("/api/conducteurs")
public class ConducteurController {

    private final ConducteurService conducteurService;

    public ConducteurController(ConducteurService conducteurService) {
        this.conducteurService = conducteurService;
    }

    @PostMapping
    public ResponseEntity<ConducteurResponse> create(@Valid @RequestBody ConducteurRequest request) {
        ConducteurResponse response = conducteurService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ConducteurResponse>> findAll() {
        return ResponseEntity.ok(conducteurService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConducteurResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(conducteurService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConducteurResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ConducteurRequest request
    ) {
        return ResponseEntity.ok(conducteurService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        conducteurService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
