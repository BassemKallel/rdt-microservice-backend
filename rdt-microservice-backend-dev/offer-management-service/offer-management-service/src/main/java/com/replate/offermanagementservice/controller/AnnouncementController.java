package com.replate.offermanagementservice.controller;

import com.replate.offermanagementservice.dto.AnnouncementRequest;
import com.replate.offermanagementservice.model.Announcement;
import com.replate.offermanagementservice.service.AnnouncementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/offers")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AnnouncementController.class);


    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    // PUBLIC - Liste
    @GetMapping("/browse")
    public ResponseEntity<List<Announcement>> browse() {
        return ResponseEntity.ok(announcementService.getAllAnnouncements());
    }

    // PUBLIC - Voir une annonce
    @GetMapping("/{id}")
    public ResponseEntity<Announcement> getById(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.getById(id));
    }

    @GetMapping("/my-offers")
    public ResponseEntity<List<Announcement>> getMyOffers(Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(announcementService.getAnnouncementsByMerchantId(merchantId));
    }

    // MERCHANT - Créer
    @PostMapping("/create")
    public ResponseEntity<Announcement> create(
            @RequestBody AnnouncementRequest request,
            @RequestHeader("X-Is-Validated") Boolean isValidated,
            Authentication authentication) {

        Long merchantId = (Long) authentication.getPrincipal();

        // Le service lèvera AccountNotValidatedException si non valide (renvoie 403).
        Announcement createdAnnouncement = announcementService.createAnnouncement(request, merchantId, isValidated);
        return ResponseEntity.ok(createdAnnouncement);
    }

    // MERCHANT - Modifier
    @PutMapping("/update/{id}")
    public ResponseEntity<Announcement> update(
            @PathVariable Long id,
            @RequestBody AnnouncementRequest request,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        Announcement updatedAnnouncement = announcementService.updateAnnouncement(id, request, userId);
        return ResponseEntity.ok(updatedAnnouncement);
    }

    // ADMIN ou MERCHANT - Supprimer
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("DELETE Request - ID: {}, UserId: {}, Role: {}", id, userId, authentication.getAuthorities());


        announcementService.deleteAnnouncement(id, userId, authentication);
        return ResponseEntity.ok("Annonce supprimée");
    }
}