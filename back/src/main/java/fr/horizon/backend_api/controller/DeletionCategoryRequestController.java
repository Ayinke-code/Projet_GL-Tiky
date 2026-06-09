package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.DeletionCategoryRequestDto;
import fr.horizon.backend_api.service.DeletionCategoryRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deletion-category-requests")
@RequiredArgsConstructor
public class DeletionCategoryRequestController {

    private final DeletionCategoryRequestService deletionCategoryRequestService;

    /**
     * Crée une demande de désinscription (pour le follower connecté)
     */
    @PostMapping("/category/{categoryId}")
    public ResponseEntity<DeletionCategoryRequestDto> createRequest(
            @PathVariable Long categoryId,
            Authentication authentication) {

        String email = authentication.getName();
        DeletionCategoryRequestDto createdRequest = deletionCategoryRequestService.createRequest(categoryId, email);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest);
    }

    /**
     * Accepte une demande (pour le manager connecté)
     */
    @PatchMapping("/{requestId}/accept")
    public ResponseEntity<DeletionCategoryRequestDto> acceptRequest(
            @PathVariable Long requestId,
            Authentication authentication) {

        String email = authentication.getName();
        DeletionCategoryRequestDto acceptedRequest = deletionCategoryRequestService.acceptRequest(requestId, email);

        return ResponseEntity.ok(acceptedRequest);
    }

    /**
     * Refuse une demande avec un motif (pour le manager connecté)
     */
    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<DeletionCategoryRequestDto> rejectRequest(
            @PathVariable Long requestId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String email = authentication.getName();
        String rejectionReason = body.get("rejectionReason");

        DeletionCategoryRequestDto rejectedRequest = deletionCategoryRequestService.rejectRequest(requestId, email, rejectionReason);

        return ResponseEntity.ok(rejectedRequest);
    }

    /**
     * Récupère toutes les demandes PENDING pour un manager donné
     */
    @GetMapping("/manager/{managerId}/pending")
    public ResponseEntity<List<DeletionCategoryRequestDto>> getPendingRequestsForManager(
            @PathVariable Long managerId) {

        List<DeletionCategoryRequestDto> pendingRequests = deletionCategoryRequestService.getPendingRequestsForManager(managerId);

        return ResponseEntity.ok(pendingRequests);
    }
}