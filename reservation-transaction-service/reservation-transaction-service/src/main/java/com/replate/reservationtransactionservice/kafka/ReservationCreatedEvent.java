package com.replate.reservationtransactionservice.kafka;

import com.replate.reservationtransactionservice.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCreatedEvent {
    private Long transactionId;
    private Long userId;
    private Long annonceId;
    private Float quantity;
    private TransactionStatus status;
}
