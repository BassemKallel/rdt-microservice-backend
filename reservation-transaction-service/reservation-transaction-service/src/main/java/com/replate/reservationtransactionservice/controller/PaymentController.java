package com.replate.reservationtransactionservice.controller;

import com.replate.reservationtransactionservice.dto.PaymentResponse;
import com.replate.reservationtransactionservice.exception.ResourceNotFoundException;
import com.replate.reservationtransactionservice.exception.UnauthorizedActionException;
import com.replate.reservationtransactionservice.model.Payment;
import com.replate.reservationtransactionservice.repository.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // --- ENDPOINT 1 : Mes Paiements (Client & Marchand) ---
    @GetMapping("/my-payments")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("INDIVIDUAL");

        List<Payment> payments;

        // Logique de filtrage selon le rôle
        if (role.contains("MERCHANT")) {
            // Le marchand voit ses encaissements
            payments = paymentRepository.findByTransaction_MerchantId(userId);
        } else {
            // Le client voit ses dépenses
            payments = paymentRepository.findByTransaction_UserId(userId);
        }

        return ResponseEntity.ok(payments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<PaymentResponse>> getAllPayments(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            throw new UnauthorizedActionException("Accès réservé aux administrateurs.");
        }

        List<Payment> payments = paymentRepository.findAll();
        return ResponseEntity.ok(payments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    // --- Méthodes existantes (Transaction & Provider) ---
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentResponse> getPaymentByTransaction(@PathVariable Long transactionId) {
        Payment payment = paymentRepository.findByTransaction_TransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable pour la transaction: " + transactionId));
        return ResponseEntity.ok(mapToResponse(payment));
    }

    // --- MAPPING ---
    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .transactionId(payment.getTransaction().getTransactionId()) // Récupère l'ID transaction
                .status(payment.getStatus())
                .amount(payment.getAmount())           // Mappé
                .createdAt(payment.getCreatedAt())     // Mappé
                .providerPaymentId(payment.getProviderPaymentId())
                .message("Détails du paiement")
                .build();
    }
}