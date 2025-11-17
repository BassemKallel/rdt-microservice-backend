package com.replate.reservationtransactionservice.kafka;

import com.replate.reservationtransactionservice.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSucceededEvent {
    private Long transactionId;
    private Float amount;
    private String providerPaymentId;
    private PaymentStatus status;
}
