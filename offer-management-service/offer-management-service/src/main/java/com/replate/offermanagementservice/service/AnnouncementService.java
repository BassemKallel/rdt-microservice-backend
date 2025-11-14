package com.replate.offermanagementservice.service;

import com.replate.offermanagementservice.dto.AnnouncementRequest;
import com.replate.offermanagementservice.kafka.AnnouncementEventProducer;
import com.replate.offermanagementservice.model.Announcement;
import com.replate.offermanagementservice.model.ModerationStatus;
import com.replate.offermanagementservice.repository.AnnouncementRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementEventProducer eventProducer;

    public AnnouncementService(AnnouncementRepository announcementRepository, AnnouncementEventProducer eventProducer) {
        this.announcementRepository = announcementRepository;
        this.eventProducer = eventProducer;
    }

    // 🚨 Mise à jour : Reçoit isValidated directement via le contrôleur (qui le lit du header)
    public Announcement createAnnouncement(AnnouncementRequest request, Long merchantId, Boolean isValidated) {

        // 2. Vérification de la Validation du Compte
        if (isValidated == null || !isValidated) {
            throw new RuntimeException("Le compte de l'utilisateur n'est pas validé et ne peut pas créer d'annonce.");
        }
        Announcement announcement = new Announcement();

        // Mappage des champs
        announcement.setMerchantId(merchantId);
        announcement.setTitle(request.getTitle());
        announcement.setDescription(request.getDescription());
        announcement.setAnnouncementType(request.getAnnouncementType());
        announcement.setPrice(request.getPrice());
        announcement.setImageUrl1(request.getImageUrl1());
        announcement.setExpiryDate(request.getExpiryDate());
        announcement.setModerationStatus(ModerationStatus.PENDING_REVIEW);

        Announcement savedAnnouncement = announcementRepository.save(announcement);

        eventProducer.sendAnnouncementEvent(savedAnnouncement, "AD_CREATED"); // Événement Kafka

        return savedAnnouncement;
    }

    // RDT-6 : Modifie une annonce existante.
    public Announcement updateAnnouncement(Long announcementId, AnnouncementRequest request, Long merchantId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Annonce non trouvée."));

        // Sécurité : Vérifier que seul le propriétaire peut modifier l'annonce
        if (!announcement.getMerchantId().equals(merchantId)) {
            throw new RuntimeException("Permission insuffisante.");
        }

        // Mises à jour des champs
        announcement.setTitle(request.getTitle());
        announcement.setDescription(request.getDescription());
        announcement.setPrice(request.getPrice());
        announcement.setUpdatedAt(LocalDateTime.now()); // Ajout de la mise à jour de la date

        Announcement updatedAnnouncement = announcementRepository.save(announcement);

        eventProducer.sendAnnouncementEvent(updatedAnnouncement, "AD_UPDATED");

        return updatedAnnouncement;
    }

    // RDT-7 : Supprime une annonce.
    public void deleteAnnouncement(Long announcementId, Long merchantId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Annonce non trouvée."));

        // Sécurité : Vérifier que seul le propriétaire peut supprimer
        if (!announcement.getMerchantId().equals(merchantId)) {
            throw new RuntimeException("Permission insuffisante.");
        }

        announcementRepository.delete(announcement);
        eventProducer.sendAnnouncementEvent(announcement, "AD_DELETED");
    }

    // RDT-13 : Lecture publique
    public List<Announcement> getAllActiveAnnouncements() {
        // En production, cette méthode devrait filtrer par ModerationStatus.ACCEPTED
        return announcementRepository.findAll();
    }
}