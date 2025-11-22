package com.replate.reservationtransactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotNull
    private Long transactionId; // Transaction linked to the payment

    private Float amount;       // Amount to pay
}
