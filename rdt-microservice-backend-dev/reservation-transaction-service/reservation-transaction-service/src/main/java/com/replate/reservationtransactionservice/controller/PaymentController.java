package com.replate.reservationtransactionservice.controller;

import com.replate.reservationtransactionservice.dto.PaymentResponse;
import com.replate.reservationtransactionservice.exception.PaymentFailedException;
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

    // Get payment by transaction ID
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentResponse> getPaymentByTransaction(@PathVariable Long transactionId) {
        Payment payment = paymentRepository.findByTransaction_TransactionId(transactionId)
                .orElseThrow(() -> new PaymentFailedException("Payment not found for transaction: " + transactionId));

        PaymentResponse response = PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .status(payment.getStatus())
                .providerPaymentId(payment.getProviderPaymentId())
                .message("Payment retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    // Get payment by Stripe provider ID
    @GetMapping("/provider/{providerPaymentId}")
    public ResponseEntity<PaymentResponse> getPaymentByProviderId(@PathVariable String providerPaymentId) {
        Payment payment = paymentRepository.findByProviderPaymentId(providerPaymentId)
                .orElseThrow(() -> new PaymentFailedException("Payment not found for provider ID: " + providerPaymentId));

        PaymentResponse response = PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .status(payment.getStatus())
                .providerPaymentId(payment.getProviderPaymentId())
                .message("Payment retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }
}
