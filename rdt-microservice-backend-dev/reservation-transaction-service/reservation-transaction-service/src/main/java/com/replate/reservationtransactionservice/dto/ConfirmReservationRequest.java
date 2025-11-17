package com.replate.reservationtransactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmReservationRequest {

    private Long transactionId; // Transaction to confirm
    private Long merchantId;    // Optional: validate the merchant
}
