package com.replate.offermanagementservice.dto;

import com.replate.offermanagementservice.model.AnnouncementType;
import com.replate.offermanagementservice.model.ModerationStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnnouncementRequest {
    private String title;
    private String description;
    private AnnouncementType announcementType;
    private ModerationStatus moderationStatus;
    private Double price;
    private String imageUrl1;
    // private String imageUrl2; // Ce champ existe dans le DTO mais pas dans le modèle
    private LocalDateTime expiryDate;
}