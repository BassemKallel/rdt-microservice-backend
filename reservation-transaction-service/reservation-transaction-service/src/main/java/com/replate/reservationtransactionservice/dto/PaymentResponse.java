package com.replate.reservationtransactionservice.dto;

import com.replate.reservationtransactionservice.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime; // Import

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long paymentId;
    private Long transactionId;       // Utile pour faire le lien
    private PaymentStatus status;
    private Float amount;             // Indispensable pour une facture
    private String providerPaymentId;
    private LocalDateTime createdAt;  // Date du paiement
    private String message;
}