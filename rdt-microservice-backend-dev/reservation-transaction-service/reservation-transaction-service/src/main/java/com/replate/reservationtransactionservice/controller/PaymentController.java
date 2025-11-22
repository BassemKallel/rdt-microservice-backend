package com.replate.reservationtransactionservice.controller;

import com.replate.reservationtransactionservice.dto.PaymentResponse;
import com.replate.reservationtransactionservice.exception.ResourceNotFoundException; // Import correct
import com.replate.reservationtransactionservice.model.Payment;
import com.replate.reservationtransactionservice.repository.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentResponse> getPaymentByTransaction(@PathVariable Long transactionId) {
        Payment payment = paymentRepository.findByTransaction_TransactionId(transactionId)
                // CORRECTION : Utiliser ResourceNotFoundException (404) au lieu de PaymentFailedException
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable pour la transaction: " + transactionId));

        return ResponseEntity.ok(mapToResponse(payment));
    }

    @GetMapping("/provider/{providerPaymentId}")
    public ResponseEntity<PaymentResponse> getPaymentByProviderId(@PathVariable String providerPaymentId) {
        Payment payment = paymentRepository.findByProviderPaymentId(providerPaymentId)
                // CORRECTION : 404
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable pour l'ID fournisseur: " + providerPaymentId));

        return ResponseEntity.ok(mapToResponse(payment));
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .status(payment.getStatus())
                .providerPaymentId(payment.getProviderPaymentId())
                .message("Payment retrieved successfully")
                .build();
    }
}