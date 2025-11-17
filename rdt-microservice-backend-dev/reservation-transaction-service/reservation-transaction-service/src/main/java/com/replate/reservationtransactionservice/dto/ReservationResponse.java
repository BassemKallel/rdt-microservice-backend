package com.replate.reservationtransactionservice.dto;

import com.replate.reservationtransactionservice.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    @NotNull
    private Long transactionId;       // ID of the created transaction

    private TransactionStatus status;
    private Boolean active;
    private Float price;
    private Float availableQuantity;
    private String message;

    public boolean isActive() { return active != null && active; }
    public boolean hasEnoughQuantity(Float q) {
        return availableQuantity != null && q != null && availableQuantity >= q;
    }
}
