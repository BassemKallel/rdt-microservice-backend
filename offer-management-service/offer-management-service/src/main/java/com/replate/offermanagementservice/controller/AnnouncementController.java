package com.replate.offermanagementservice.controller;

import com.replate.offermanagementservice.dto.AnnouncementRequest;
import com.replate.offermanagementservice.dto.AnnouncementResponseDTO;
import com.replate.offermanagementservice.model.Announcement;
import com.replate.offermanagementservice.service.AnnouncementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/offers")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private static final Logger log = LoggerFactory.getLogger(AnnouncementController.class);

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    // --- ENDPOINTS PUBLICS / LECTURE ---

    // Point d'entrée principal pour le catalogue (Filtrage auto par rôle)
    @GetMapping("/browse")
    public ResponseEntity<List<AnnouncementResponseDTO>> browse(Authentication authentication) {
        String role = null;
        if (authentication != null && !authentication.getAuthorities().isEmpty()) {
            role = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse(null);
        }
        return ResponseEntity.ok(announcementService.getAnnouncementsBasedOnRole(role));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.getByIdWithMerchant(id));
    }

    // --- ENDPOINTS MARCHAND ---

    @GetMapping("/my-offers")
    public ResponseEntity<List<Announcement>> getMyOffers(Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        // Ici on peut renvoyer l'entité brute (le marchand connait son nom)
        return ResponseEntity.ok(announcementService.getAnnouncementsByMerchantId(merchantId));
    }

    @PostMapping("/create")
    public ResponseEntity<Announcement> create(
            @Valid @RequestBody AnnouncementRequest request,
            Authentication authentication) {

        Long merchantId = (Long) authentication.getPrincipal();
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) authentication.getDetails();
        String status = details != null ? details.getOrDefault("status", "PENDING") : "PENDING";
        boolean isValidated = "ACTIVE".equalsIgnoreCase(status) || "true".equalsIgnoreCase(status);

        log.info("Création Annonce - MerchantID: {}, Stock: {}", merchantId, request.getStock());

        return ResponseEntity.ok(announcementService.createAnnouncement(request, merchantId, isValidated));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Announcement> update(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementRequest request,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(announcementService.updateAnnouncement(id, request, userId));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        announcementService.deleteAnnouncement(id, userId, authentication);
        return ResponseEntity.ok("Annonce supprimée avec succès.");
    }

    // --- ENDPOINTS INTERNES (RTS) ---

    @PostMapping("/{id}/decrease-stock")
    public ResponseEntity<Void> decreaseStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {

        announcementService.decreaseStock(id, quantity);
        return ResponseEntity.ok().build();
    }
}