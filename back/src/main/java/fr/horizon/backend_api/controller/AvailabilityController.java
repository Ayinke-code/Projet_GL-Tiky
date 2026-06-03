package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.AvailabilityDTO;
import fr.horizon.backend_api.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/availabilities")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<AvailabilityDTO>> getMyAvailabilities(Authentication authentication) {
        return ResponseEntity.ok(availabilityService.findByUserEmail(authentication.getName()));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<AvailabilityDTO> createAvailability(
            @RequestBody AvailabilityDTO availability,
            Authentication authentication) {
        AvailabilityDTO created = availabilityService.createAvailability(availability, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable Long id, Authentication authentication) {
        availabilityService.deleteAvailability(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
