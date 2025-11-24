package com.replate.offermanagementservice.dto;

import com.replate.offermanagementservice.model.Announcement;
import com.replate.offermanagementservice.model.AnnouncementType;
import com.replate.offermanagementservice.model.FoodCategory;
import com.replate.offermanagementservice.model.Unit;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnnouncementResponseDTO {
    private Long id;
    private String title;
    private String description;
    private Double price;
    private Double stock;
    private String imageUrl1;
    private AnnouncementType announcementType;
    private FoodCategory category;
    private Unit unit;
    private LocalDateTime expiryDate;

    // --- Infos Marchand ---
    private Long merchantId;
    private String merchantName; // Le champ que vous voulez ajouter

    // Constructeur utilitaire
    public static AnnouncementResponseDTO fromEntity(Announcement a, String merchantName) {
        AnnouncementResponseDTO dto = new AnnouncementResponseDTO();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setDescription(a.getDescription());
        dto.setPrice(a.getPrice());
        dto.setStock(a.getStock());
        dto.setImageUrl1(a.getImageUrl1());
        dto.setAnnouncementType(a.getAnnouncementType());
        dto.setCategory(a.getCategory());
        dto.setUnit(a.getUnit());
        dto.setExpiryDate(a.getExpiryDate());
        dto.setMerchantId(a.getMerchantId());
        dto.setMerchantName(merchantName);
        return dto;
    }
}