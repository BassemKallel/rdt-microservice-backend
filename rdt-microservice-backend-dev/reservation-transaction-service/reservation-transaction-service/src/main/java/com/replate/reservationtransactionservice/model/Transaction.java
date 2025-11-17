package com.replate.reservationtransactionservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(nullable = false)
    private Long annonceId;

    @Column(nullable = false)
    private Long userId; // beneficiary

    @Column(nullable = false)
    private Long merchantId; // merchant who posted announcement

    @Column(nullable = false)
    private Float quantiteTransmise;

    @Column(nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.PENDING_CONFIRMATION;

    @Enumerated(EnumType.STRING)
    private OfferType offerType;

    // Optional Payment for SALE transactions
    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private Payment payment;
}
