package com.replate.reservationtransactionservice.controller;

import com.replate.reservationtransactionservice.model.Payment;
import com.replate.reservationtransactionservice.model.PaymentStatus;
import com.replate.reservationtransactionservice.model.Transaction;
import com.replate.reservationtransactionservice.model.TransactionStatus;
import com.replate.reservationtransactionservice.repository.PaymentRepository;
import com.replate.reservationtransactionservice.repository.TransactionRepository;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/webhook")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

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
        // DEBUG : Confirmation brute que la requête arrive
        System.out.println(">>> Webhook reçu ! Signature: " + sigHeader);

        Event event;

        try {
            // 1. Vérification de la signature
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (Exception e) {
            log.error("❌ Erreur de signature Webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook Error: " + e.getMessage());
        }

        // Log du type d'événement reçu
        log.info("🔍 Événement Stripe reçu: {}", event.getType());

        // 2. Gestion de l'événement "Paiement Réussi"
        if ("payment_intent.succeeded".equals(event.getType())) {

            // Tentative de désérialisation
            StripeObject stripeObject = event.getData().getObject();
            if (stripeObject instanceof PaymentIntent) {
                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                log.info("💰 Paiement détecté (ID: {}). Traitement...", paymentIntent.getId());
                handlePaymentSuccess(paymentIntent);
            } else {
                log.error("❌ L'objet reçu n'est pas un PaymentIntent valide ou est null.");
            }
        } else {
            log.debug("Événement ignoré: {}", event.getType());
        }

        return ResponseEntity.ok("Received");
    }

    private void handlePaymentSuccess(PaymentIntent paymentIntent) {
        // 3. Recherche du paiement en base
        log.info("🔎 Recherche du paiement local pour providerId: {}", paymentIntent.getId());

        Payment payment = paymentRepository.findByProviderPaymentId(paymentIntent.getId())
                .orElse(null);

        // 🚨 GESTION RACE CONDITION
        if (payment == null) {
            log.error("⏳ Paiement introuvable en BDD (Race Condition). On renvoie 404 pour forcer Stripe à réessayer plus tard.");
            // Renvoie 404 à Stripe -> Stripe réessaiera dans quelques secondes/minutes
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found yet - Retry later");
        }

        // 4. Mise à jour (Succès)
        log.info("✅ Paiement trouvé (ID: {}). Mise à jour du statut...", payment.getPaymentId());

        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);

        // 5. Validation de la transaction
        Transaction transaction = payment.getTransaction();
        if (transaction != null) {
            transaction.setStatus(TransactionStatus.CONFIRMED);
            transactionRepository.save(transaction);
            log.info("🚀 Transaction #{} confirmée et finalisée !", transaction.getTransactionId());
        } else {
            log.warn("⚠️ Paiement orphelin : aucune transaction associée.");
        }
    }
}