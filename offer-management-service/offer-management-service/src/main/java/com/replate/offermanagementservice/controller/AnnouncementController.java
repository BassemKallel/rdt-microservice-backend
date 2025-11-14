package com.replate.offermanagementservice.controller;

import com.replate.offermanagementservice.dto.AnnouncementRequest;
import com.replate.offermanagementservice.model.Announcement;
import com.replate.offermanagementservice.service.AnnouncementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/offers")
public class AnnouncementController {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementController.class);

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createAnnouncement(
            @RequestBody AnnouncementRequest announcementDTO,

            // 🚨 Lit les claims utilisateur depuis les headers injectés par le Gateway
            @RequestHeader("X-User-Id") Long merchantId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-Is-Validated") Boolean isValidated) {

        log.info("🎯 Création annonce - MerchantID: {}, Role: {}, Validated: {}",
                merchantId, userRole, isValidated);

        // Passe l'état de validation au service
        Announcement created = announcementService.createAnnouncement(announcementDTO, merchantId, isValidated);
        return ResponseEntity.ok(created);
    }

    // Endpoint de mise à jour (Exemple)
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody @Valid AnnouncementRequest announcementDTO,
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestHeader("X-User-Role") String userRole) {

        if (!"MERCHANT".equals(userRole)) {
            return ResponseEntity.status(403).body("Seuls les MERCHANTS peuvent modifier des annonces");
        }

        try {
            Announcement updated = announcementService.updateAnnouncement(id, announcementDTO, currentUserId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            // Gérer les cas où l'annonce n'est pas trouvée ou l'utilisateur n'est pas propriétaire
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}