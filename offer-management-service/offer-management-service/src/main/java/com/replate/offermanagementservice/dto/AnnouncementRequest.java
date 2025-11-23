package com.replate.offermanagementservice.dto;

import com.replate.offermanagementservice.model.AnnouncementType;
import com.replate.offermanagementservice.model.FoodCategory;
import com.replate.offermanagementservice.model.ModerationStatus;
import com.replate.offermanagementservice.model.Unit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnnouncementRequest {
    private String title;
    private String description;
    @NotNull(message = "Type est obligatoire")
    private AnnouncementType announcementType;
    private Double price;
    private String imageUrl1;
    private LocalDateTime expiryDate;

    @NotNull(message = "Stcok est obligatoire")
    @Min(value = 1, message = "Le stock doit être au moins de 1.")
    private Double stock;

    @NotNull(message = "Category est obligatoire")
    private FoodCategory category;

    @NotNull(message = "Unit est obligatoire")
    private Unit unit;
}