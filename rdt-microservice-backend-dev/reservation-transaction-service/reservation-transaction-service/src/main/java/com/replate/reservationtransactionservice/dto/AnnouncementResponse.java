package com.replate.reservationtransactionservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder

public class AnnouncementResponse {
    private Long id;
    private Long merchantId;
    private String title;
    private Double price;

    // On mappe uniquement ce dont on a besoin
}