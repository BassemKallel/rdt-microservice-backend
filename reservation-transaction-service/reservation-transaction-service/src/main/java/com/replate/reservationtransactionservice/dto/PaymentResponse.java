package com.replate.reservationtransactionservice.dto;

import com.replate.reservationtransactionservice.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long paymentId;           // Payment record ID
    private PaymentStatus status;     // PENDING, COMPLETED, REFUNDED
    private String providerPaymentId; // External gateway reference
    private String message;           // Info message for client or microservices

}
