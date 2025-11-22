package com.replate.offermanagementservice.controller;

import com.replate.offermanagementservice.dto.AnnouncementRequest;
import com.replate.offermanagementservice.model.Announcement;
import com.replate.offermanagementservice.service.AnnouncementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    // --- LECTURE ---

    @GetMapping("/browse")
    public ResponseEntity<List<Announcement>> browse() {
        return ResponseEntity.ok(announcementService.getAllAnnouncements());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Announcement> getById(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.getById(id));
    }

    @GetMapping("/my-offers")
    public ResponseEntity<List<Announcement>> getMyOffers(Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(announcementService.getAnnouncementsByMerchantId(merchantId));
    }


    @PostMapping("/create")
    public ResponseEntity<Announcement> create(
            @RequestBody AnnouncementRequest request,
            Authentication authentication) {

        Long merchantId = (Long) authentication.getPrincipal();

        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) authentication.getDetails();

        String status = details != null ? details.getOrDefault("status", "PENDING") : "PENDING";

        boolean isValidated = "ACTIVE".equalsIgnoreCase(status) || "true".equalsIgnoreCase(status);

        log.info("Création Annonce - MerchantID: {}, Status reçu: {}, Validé: {}", merchantId, status, isValidated);

        Announcement createdAnnouncement = announcementService.createAnnouncement(request, merchantId, isValidated);

        return ResponseEntity.ok(createdAnnouncement);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Announcement> update(
            @PathVariable Long id,
            @RequestBody AnnouncementRequest request,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("Mise à jour annonce #{} par User #{}", id, userId);

        return ResponseEntity.ok(announcementService.updateAnnouncement(id, request, userId));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("Suppression annonce #{} par User #{}", id, userId);

        announcementService.deleteAnnouncement(id, userId, authentication);
        return ResponseEntity.ok("Annonce supprimée avec succès.");
    }
}