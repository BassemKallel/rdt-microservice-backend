package com.replate.reservationtransactionservice.controller;

import com.replate.reservationtransactionservice.model.Payment;
import com.replate.reservationtransactionservice.model.PaymentStatus;
import com.replate.reservationtransactionservice.model.Transaction;
import com.replate.reservationtransactionservice.model.TransactionStatus;
import com.replate.reservationtransactionservice.repository.PaymentRepository;
import com.replate.reservationtransactionservice.repository.TransactionRepository;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    // Clé secrète de signature (whsec_...) définie dans application.properties
    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;

    public StripeWebhookController(PaymentRepository paymentRepository, TransactionRepository transactionRepository) {
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping
    public ResponseEntity<String> handleStripeEvent(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;

        try {
            // 1. Vérification cryptographique de la signature
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (Exception e) {
            log.error("❌ Erreur de signature Webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook Error: " + e.getMessage());
        }

        // 2. Traitement de l'événement de succès de paiement
        if ("payment_intent.succeeded".equals(event.getType())) {
            // Extraction sécurisée de l'objet Stripe
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);

            if (paymentIntent != null) {
                log.info("💰 Événement Stripe reçu : Paiement {} réussi", paymentIntent.getId());
                handlePaymentSuccess(paymentIntent);
            }
        }

        return ResponseEntity.ok("Received");
    }

    private void handlePaymentSuccess(PaymentIntent paymentIntent) {
        // 3. Retrouver le paiement en base via l'ID Stripe (pi_...)
        Payment payment = paymentRepository.findByProviderPaymentId(paymentIntent.getId())
                .orElse(null);

        if (payment != null) {
            // 4. Mettre à jour le statut du paiement
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);

            // 5. Mettre à jour la transaction liée
            Transaction transaction = payment.getTransaction();
            if (transaction != null) {
                transaction.setStatus(TransactionStatus.CONFIRMED);
                transactionRepository.save(transaction);
                log.info("✅ Transaction #{} confirmée et payée.", transaction.getTransactionId());
            }
        } else {
            log.warn("⚠️ Paiement introuvable pour l'ID Stripe: {}", paymentIntent.getId());
        }
    }
}