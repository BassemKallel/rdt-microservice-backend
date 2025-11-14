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
            @RequestBody @Valid AnnouncementRequest announcementDTO,
            // 🚨 Lit les claims utilisateur depuis les headers injectés par le Gateway
            @RequestHeader("X-User-Id") Long merchantId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-Is-Validated") Boolean isValidated) {

        log.info("🎯 Création annonce - MerchantID: {}, Role: {}, Validated: {}",
                merchantId, userRole, isValidated);

        // Vérification de sécurité basée sur le rôle
        if (!"MERCHANT".equals(userRole)) {
            return ResponseEntity.status(403).body("Seuls les MERCHANTS peuvent créer des annonces");
        }

        Announcement created = announcementService.createAnnouncement(announcementDTO, merchantId, isValidated);
        return ResponseEntity.ok(created);
    }

    // Ajoutez ici d'autres endpoints CRUD...
    // Exemple pour la modification :
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody @Valid AnnouncementRequest announcementDTO,
            @RequestHeader("X-User-Id") Long currentUserId) {

        Announcement updated = announcementService.updateAnnouncement(id, announcementDTO, currentUserId);
        return ResponseEntity.ok(updated);
    }
}