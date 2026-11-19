package ma.youcode.covoiturage.controller;

import jakarta.validation.Valid;
import java.util.List;
import ma.youcode.covoiturage.dto.passager.PassagerRequest;
import ma.youcode.covoiturage.dto.passager.PassagerResponse;
import ma.youcode.covoiturage.service.PassagerService;
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
@RequestMapping("/api/passagers")
public class PassagerController {

    private final PassagerService passagerService;

    public PassagerController(PassagerService passagerService) {
        this.passagerService = passagerService;
    }

    @PostMapping
    public ResponseEntity<PassagerResponse> create(@Valid @RequestBody PassagerRequest request) {
        PassagerResponse response = passagerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PassagerResponse>> findAll() {
        return ResponseEntity.ok(passagerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PassagerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(passagerService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PassagerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PassagerRequest request
    ) {
        return ResponseEntity.ok(passagerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        passagerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
