package com.replate.reservationtransactionservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    // Each Payment must belong to exactly one Transaction
    @OneToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(nullable = false)
    private Float amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "provider_payment_id", nullable = false)
    private String providerPaymentId; // external gateway ID (Stripe, etc.)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
