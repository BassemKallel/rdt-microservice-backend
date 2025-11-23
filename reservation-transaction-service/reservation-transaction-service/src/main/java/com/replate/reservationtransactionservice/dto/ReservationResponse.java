package com.replate.reservationtransactionservice.dto;

import com.replate.reservationtransactionservice.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime; // <--- IMPORT IMPORTANT

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {
    private Long transactionId;
    private TransactionStatus status;
    private Boolean active;
    private Float price;
    private Float availableQuantity;
    private String message;
    private String paymentClientSecret;

    // --- NOUVEAUX CHAMPS POUR L'HISTORIQUE ---
    private LocalDateTime transactionDate; // Date de la commande
    private Long userId;                   // ID de l'acheteur
    private Long announcementId;           // ID de l'annonce
    private String announcementTitle;      // Titre de l'annonce (ex: "Panier de légumes")

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}