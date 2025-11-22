package com.replate.reservationtransactionservice.dto;

import com.replate.reservationtransactionservice.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {
    private Long transactionId;       // ID of the created transaction

    private TransactionStatus status;
    private Boolean active;
    private Float price;
    private Float availableQuantity;
    private String message;
    private String paymentClientSecret;


    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
