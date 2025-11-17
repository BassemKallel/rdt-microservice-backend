package com.replate.reservationtransactionservice.dto;

import com.replate.reservationtransactionservice.model.OfferType;
import org.antlr.v4.runtime.misc.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequest {

    @NotNull
    private Long annonceId;

    @NotNull
    private Long userId;              // Beneficiary ID

    private Float quantiteTransmise;
    private OfferType offerType;
    private Float amount;

}