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
            // 1. Vérification de la signature (Sécurité critique)
            // Cela garantit que la requête vient bien de Stripe et non d'un pirate
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (Exception e) {
            log.error("❌ Erreur de signature Webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook Error: " + e.getMessage());
        }

        // 2. Gestion de l'événement "Paiement Réussi"
        if ("payment_intent.succeeded".equals(event.getType())) {
            // Désérialisation de l'objet Stripe
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);

            if (paymentIntent != null) {
                log.info("💰 Événement Stripe reçu : Paiement {} réussi", paymentIntent.getId());
                handlePaymentSuccess(paymentIntent);
            }
        }

        return ResponseEntity.ok("Received");
    }

    private void handlePaymentSuccess(PaymentIntent paymentIntent) {
        // 3. Retrouver le paiement via l'ID Stripe (pi_...)
        Payment payment = paymentRepository.findByProviderPaymentId(paymentIntent.getId())
                .orElse(null);

        if (payment != null) {
            // 4. Mettre à jour le statut du paiement
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);

            // 5. Valider la transaction liée
            Transaction transaction = payment.getTransaction();
            if (transaction != null) {
                transaction.setStatus(TransactionStatus.CONFIRMED);
                transactionRepository.save(transaction);
                log.info("✅ Transaction #{} confirmée suite au paiement.", transaction.getTransactionId());

                // (Optionnel) Ici, vous pourriez envoyer un événement Kafka "TransactionConfirmedEvent"
                // pour notifier les autres services (ex: envoyer un email de confirmation).
            }
        } else {
            log.warn("⚠️ Paiement introuvable en base pour l'ID Stripe: {}", paymentIntent.getId());
        }
    }
}