package com.replate.offermanagementservice.dto;

import com.replate.offermanagementservice.model.AnnouncementType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnouncementRequest {
    private String title;
    private String description;
    private AnnouncementType announcementType;

    // Géolocalisation et prix
    private Double price;

    // URL de l'image (obtenue du file-service)
    private String imageUrl1;
    private String imageUrl2;

    // Dates
    private LocalDateTime expiryDate;
}