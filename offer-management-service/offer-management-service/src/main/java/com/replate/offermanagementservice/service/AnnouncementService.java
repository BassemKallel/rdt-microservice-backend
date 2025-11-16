package com.replate.offermanagementservice.service;

import com.replate.offermanagementservice.dto.AnnouncementRequest;
// Imports pour les nouvelles exceptions
import com.replate.offermanagementservice.exception.AccountNotValidatedException;
import com.replate.offermanagementservice.exception.ForbiddenPermissionException;
import com.replate.offermanagementservice.exception.ResourceNotFoundException;
import com.replate.offermanagementservice.model.Announcement;
import com.replate.offermanagementservice.model.ModerationStatus;
import com.replate.offermanagementservice.repository.AnnouncementRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    // Lister toutes les annonces (public)
    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAll();
    }

    // Lister les annonces pour un marchand spécifique
    public List<Announcement> getAnnouncementsByMerchantId(Long merchantId) {
        return announcementRepository.findAllByMerchantId(merchantId);
    }

    // Voir une annonce (public)
    public Announcement getById(Long id) {
        return announcementRepository.findById(id)
                // MISE À JOUR: Lève une exception spécifique
                .orElseThrow(() -> new ResourceNotFoundException("Annonce non trouvée avec l'ID: " + id));
    }

    // Créer une annonce (MERCHANT)
    public Announcement createAnnouncement(AnnouncementRequest request, Long merchantId, Boolean isValidated) {
        if (isValidated == null || !isValidated) {
            // MISE À JOUR: Lève une exception spécifique
            throw new AccountNotValidatedException("Le compte n'est pas validé et ne peut pas créer d'annonce.");
        }

        Announcement announcement = new Announcement();
        announcement.setMerchantId(merchantId);
        announcement.setTitle(request.getTitle());
        announcement.setDescription(request.getDescription());
        announcement.setPrice(request.getPrice());
        announcement.setAnnouncementType(request.getAnnouncementType());
        announcement.setModerationStatus(ModerationStatus.PENDING_REVIEW);
        announcement.setImageUrl1(request.getImageUrl1());
        announcement.setUpdatedAt(LocalDateTime.now());
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setExpiryDate(request.getExpiryDate());

        return announcementRepository.save(announcement);
    }

    // Modifier une annonce (MERCHANT - propriétaire uniquement)
    public Announcement updateAnnouncement(Long id, AnnouncementRequest request, Long currentUserId) {
        Announcement announcement = announcementRepository.findById(id)
                // MISE À JOUR: Lève une exception spécifique
                .orElseThrow(() -> new ResourceNotFoundException("Annonce non trouvée avec l'ID: " + id));

        if (!announcement.getMerchantId().equals(currentUserId)) {
            throw new ForbiddenPermissionException("Vous ne pouvez modifier que vos propres annonces.");
        }

        // ... (logique de mise à jour partielle)
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            announcement.setTitle(request.getTitle());
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            announcement.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            announcement.setPrice(request.getPrice());
        }
        if (request.getAnnouncementType() != null) {
            announcement.setAnnouncementType(request.getAnnouncementType());
        }
        if (request.getImageUrl1() != null && !request.getImageUrl1().trim().isEmpty()) {
            announcement.setImageUrl1(request.getImageUrl1());
        }
        if (request.getExpiryDate() != null) {
            announcement.setExpiryDate(request.getExpiryDate());
        }

        announcement.setModerationStatus(ModerationStatus.PENDING_REVIEW);
        announcement.setUpdatedAt(LocalDateTime.now());
        return announcementRepository.save(announcement);
    }

    public void deleteAnnouncement(Long id, Long currentUserId, Authentication authentication) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Annonce non trouvée avec l'ID: " + id));

        // ADMIN peut tout supprimer
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        if (isAdmin) {
            announcementRepository.delete(announcement);
            return;
        }

        // MERCHANT peut supprimer seulement ses annonces
        boolean isMerchant = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_MERCHANT"));

        if (isMerchant && announcement.getMerchantId().equals(currentUserId)) {
            announcementRepository.delete(announcement);
            return;
        }
        throw new ForbiddenPermissionException("Permission refusée pour supprimer cette ressource.");
    }
}