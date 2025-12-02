package com.replate.offermanagementservice.service;

import com.replate.offermanagementservice.client.UserClient;
import com.replate.offermanagementservice.dto.AnnouncementRequest;
import com.replate.offermanagementservice.dto.AnnouncementResponseDTO;
import com.replate.offermanagementservice.dto.UserDTO;
import com.replate.offermanagementservice.exception.AccountNotValidatedException;
import com.replate.offermanagementservice.exception.ForbiddenPermissionException;
import com.replate.offermanagementservice.exception.InsufficientStockException;
import com.replate.offermanagementservice.exception.ResourceNotFoundException;
import com.replate.offermanagementservice.kafka.AnnouncementEventProducer;
import com.replate.offermanagementservice.model.Announcement;
import com.replate.offermanagementservice.model.AnnouncementType;
import com.replate.offermanagementservice.model.ModerationStatus;
import com.replate.offermanagementservice.repository.AnnouncementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnnouncementService {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementService.class);

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementEventProducer eventProducer;
    private final UserClient userClient; // Client Feign pour UMS

    public AnnouncementService(AnnouncementRepository announcementRepository,
                               AnnouncementEventProducer eventProducer,
                               UserClient userClient) {
        this.announcementRepository = announcementRepository;
        this.eventProducer = eventProducer;
        this.userClient = userClient;
    }

    // --- LECTURE (Avec Enrichissement et Filtrage) ---

    /**
     * Récupère les annonces filtrées par rôle et enrichies avec le nom du marchand.
     */
    public List<AnnouncementResponseDTO> getAnnouncementsBasedOnRole(String userRole) {
        List<Announcement> announcements;

        // 1. Logique de Filtrage
        if (userRole == null) {
            announcements = announcementRepository.findAll();
        } else {
            String normalizedRole = userRole.replace("ROLE_", "").trim().toUpperCase();
            switch (normalizedRole) {
                case "INDIVIDUAL":
                    announcements = announcementRepository.findByAnnouncementType(AnnouncementType.SALE);
                    break;
                case "ASSOCIATION":
                    announcements = announcementRepository.findByAnnouncementType(AnnouncementType.DONATION);
                    break;
                default:
                    announcements = announcementRepository.findAll();
                    break;
            }
        }

        return announcements.stream()
                .map(this::enrichWithMerchantName)
                .collect(Collectors.toList());
    }

    /**
     * Récupère une annonce par ID avec les détails du marchand.
     */
    public AnnouncementResponseDTO getByIdWithMerchant(Long id) {
        Announcement announcement = getById(id);
        return enrichWithMerchantName(announcement);
    }

    public List<Announcement> getAnnouncementsByMerchantId(Long merchantId) {
        return announcementRepository.findAllByMerchantId(merchantId);
    }

    // Méthode interne pour récupérer l'entité brute
    public Announcement getById(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Annonce non trouvée avec l'ID: " + id));
    }

    // --- ÉCRITURE ---

    @Transactional
    public Announcement createAnnouncement(AnnouncementRequest request, Long merchantId, Boolean isValidated) {
        if (isValidated == null || !isValidated) {
            log.warn("Tentative de création par compte non validé (ID: {})", merchantId);
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

        // Nouveaux champs
        announcement.setCategory(request.getCategory());
        announcement.setUnit(request.getUnit());
        announcement.setStock(request.getStock());

        announcement.setModerationStatus(ModerationStatus.PENDING_REVIEW);
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setUpdatedAt(LocalDateTime.now());

        Announcement saved = announcementRepository.save(announcement);
        log.info("Annonce créée (ID: {}) par Marchand (ID: {})", saved.getId(), merchantId);

        if (eventProducer != null) {
            eventProducer.sendAnnouncementEvent(saved, "ANNOUNCEMENT_CREATED");
        }

        return saved;
    }

    @Transactional
    public Announcement updateAnnouncement(Long id, AnnouncementRequest request, Long currentUserId) {
        Announcement announcement = getById(id);

        if (!announcement.getMerchantId().equals(currentUserId)) {
            throw new ForbiddenPermissionException("Vous ne pouvez modifier que vos propres annonces.");
        }

        if (request.getTitle() != null) announcement.setTitle(request.getTitle());
        if (request.getDescription() != null) announcement.setDescription(request.getDescription());
        if (request.getPrice() != null) announcement.setPrice(request.getPrice());
        if (request.getAnnouncementType() != null) announcement.setAnnouncementType(request.getAnnouncementType());
        if (request.getImageUrl1() != null) announcement.setImageUrl1(request.getImageUrl1());
        if (request.getExpiryDate() != null) announcement.setExpiryDate(request.getExpiryDate());

        // Mise à jour nouveaux champs
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

    // --- LOGIQUE MÉTIER (STOCK) ---

    @Transactional
    public void decreaseStock(Long announcementId, Integer quantity) {
        Announcement announcement = getById(announcementId);

        if (announcement.getStock() < quantity) {
            throw new InsufficientStockException("Stock insuffisant pour l'annonce " + announcementId);
        }

        announcement.setStock(announcement.getStock() - quantity);
        announcementRepository.save(announcement);
        log.info("Stock décrémenté pour annonce {}. Reste: {}", announcementId, announcement.getStock());
    }

    // --- UTILITAIRES ---

    private AnnouncementResponseDTO enrichWithMerchantName(Announcement announcement) {
        String merchantName = "Inconnu";
        try {
            UserDTO user = userClient.getUserById(announcement.getMerchantId());
            if (user != null) {
                merchantName = user.getUsername();
            }
        } catch (Exception e) {
            // Fallback silencieux si UMS est down
            log.warn("Impossible de récupérer le nom du marchand pour ID {}", announcement.getMerchantId());
        }
        return AnnouncementResponseDTO.fromEntity(announcement, merchantName);
    }

    @Transactional
    public void increaseStock(Long announcementId, Integer quantity) {
        Announcement announcement = getById(announcementId);
        announcement.setStock(announcement.getStock() + quantity);
        announcementRepository.save(announcement);
        log.info("Stock ré-incrémenté (Compensation) pour annonce {}. Nouveau stock: {}", announcementId, announcement.getStock());
    }
}