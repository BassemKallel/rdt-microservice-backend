package com.replate.offermanagementservice.service;

import com.replate.offermanagementservice.dto.AnnouncementRequest;
import com.replate.offermanagementservice.exception.AccountNotValidatedException;
import com.replate.offermanagementservice.exception.ForbiddenPermissionException;
import com.replate.offermanagementservice.exception.InsufficientStockException;
import com.replate.offermanagementservice.exception.ResourceNotFoundException;
import com.replate.offermanagementservice.model.Announcement;
import com.replate.offermanagementservice.model.ModerationStatus;
import com.replate.offermanagementservice.repository.AnnouncementRepository;
import com.replate.offermanagementservice.kafka.AnnouncementEventProducer; // Optional: if you want to emit events
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnnouncementService {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementService.class);

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementEventProducer eventProducer; // Optional

    public AnnouncementService(AnnouncementRepository announcementRepository, AnnouncementEventProducer eventProducer) {
        this.announcementRepository = announcementRepository;
        this.eventProducer = eventProducer;
    }

    // --- LECTURE ---

    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAll();
    }

    public List<Announcement> getAnnouncementsByMerchantId(Long merchantId) {
        return announcementRepository.findAllByMerchantId(merchantId);
    }

    public Announcement getById(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Annonce non trouvée avec l'ID: " + id));
    }

    // --- ÉCRITURE ---

    @Transactional
    public Announcement createAnnouncement(AnnouncementRequest request, Long merchantId, Boolean isValidated) {
        // 1. Vérification du statut du compte
        if (isValidated == null || !isValidated) {
            log.warn("Tentative de création d'annonce par un compte non validé (ID: {})", merchantId);
            throw new AccountNotValidatedException("Votre compte n'est pas validé. Vous ne pouvez pas publier d'offres.");
        }

        Announcement announcement = new Announcement();
        announcement.setMerchantId(merchantId);
        announcement.setTitle(request.getTitle());
        announcement.setDescription(request.getDescription());
        announcement.setPrice(request.getPrice());
        announcement.setAnnouncementType(request.getAnnouncementType());
        announcement.setImageUrl1(request.getImageUrl1());
        announcement.setExpiryDate(request.getExpiryDate());

        announcement.setModerationStatus(ModerationStatus.ACCEPTED);
        announcement.setUnit(request.getUnit());
        announcement.setStock(request.getStock());
        announcement.setCategory(request.getCategory());
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setUpdatedAt(LocalDateTime.now());

        Announcement saved = announcementRepository.save(announcement);
        log.info("Annonce créée avec succès (ID: {}) par Marchand (ID: {})", saved.getId(), merchantId);

        // 3. (Optionnel) Envoyer un événement Kafka
        if (eventProducer != null) {
            eventProducer.sendAnnouncementEvent(saved, "ANNOUNCEMENT_CREATED");
        }

        return saved;
    }

    @Transactional
    public Announcement updateAnnouncement(Long id, AnnouncementRequest request, Long currentUserId) {
        Announcement announcement = getById(id);

        // Vérification de la propriété
        if (!announcement.getMerchantId().equals(currentUserId)) {
            throw new ForbiddenPermissionException("Vous ne pouvez modifier que vos propres annonces.");
        }

        // Mise à jour partielle (Patch-like logic)
        if (request.getTitle() != null) announcement.setTitle(request.getTitle());
        if (request.getDescription() != null) announcement.setDescription(request.getDescription());
        if (request.getPrice() != null) announcement.setPrice(request.getPrice());
        if (request.getAnnouncementType() != null) announcement.setAnnouncementType(request.getAnnouncementType());
        if (request.getImageUrl1() != null) announcement.setImageUrl1(request.getImageUrl1());
        if (request.getExpiryDate() != null) announcement.setExpiryDate(request.getExpiryDate());
        if (request.getCategory() != null) announcement.setCategory(request.getCategory());
        if (request.getUnit() != null) announcement.setUnit(request.getUnit());
        if (request.getStock() != null) announcement.setStock(request.getStock());


        announcement.setUpdatedAt(LocalDateTime.now());

        return announcementRepository.save(announcement);
    }

    @Transactional
    public void deleteAnnouncement(Long id, Long currentUserId, Authentication authentication) {
        Announcement announcement = getById(id);

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        boolean isOwner = announcement.getMerchantId().equals(currentUserId);

        if (!isAdmin && !isOwner) {
            throw new ForbiddenPermissionException("Vous n'avez pas la permission de supprimer cette annonce.");
        }

        announcementRepository.delete(announcement);
        log.info("Annonce #{} supprimée par User #{}", id, currentUserId);

        if (eventProducer != null) {
            eventProducer.sendAnnouncementEvent(announcement, "ANNOUNCEMENT_DELETED");
        }
    }

    @Transactional
    public void decreaseStock(Long announcementId, Integer quantity) {
        Announcement announcement = getById(announcementId);

        if (announcement.getStock() < quantity) {
            throw new InsufficientStockException("Stock insuffisant pour l'annonce " + announcementId +
                    ". Disponible: " + announcement.getStock() +
                    ", Demandé: " + quantity);
        }

        announcement.setStock(announcement.getStock() - quantity);
        announcementRepository.save(announcement);

        log.info("Stock décrémenté pour l'annonce {}. Nouveau stock: {}", announcementId, announcement.getStock());
    }
}