package com.replate.reservationtransactionservice.kafka;

import com.replate.reservationtransactionservice.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionConfirmedEvent {
    private Long transactionId;
    private Long userId;
    private Long annonceId;
    private TransactionStatus status;
}