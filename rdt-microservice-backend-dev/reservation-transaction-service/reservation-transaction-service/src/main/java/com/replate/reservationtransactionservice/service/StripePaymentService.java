package com.replate.reservationtransactionservice.service;

import com.replate.reservationtransactionservice.exception.PaymentFailedException;
import com.replate.reservationtransactionservice.model.Payment;
import com.replate.reservationtransactionservice.model.PaymentStatus;
import com.replate.reservationtransactionservice.model.Transaction;
import com.replate.reservationtransactionservice.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripePaymentService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.currency}")
    private String currency;

    private final PaymentRepository paymentRepository;

    public StripePaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    /**
     * Crée une intention de paiement Stripe et sauvegarde le paiement en statut PENDING.
     */
    @Transactional
    public PaymentIntent createPaymentIntent(Transaction transaction, Float amount) {
        // Stripe utilise les centimes (ex: 10.00 EUR -> 1000 cents)
        long amountInCents = (long) (amount * 100);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency)
                .setDescription("Transaction Replate #" + transaction.getTransactionId())
                // On attache l'ID de transaction aux métadonnées pour le retrouver via le Webhook plus tard
                .putMetadata("transaction_id", transaction.getTransactionId().toString())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                )
                .build();

        try {
            PaymentIntent intent = PaymentIntent.create(params);

            // On crée l'enregistrement en base locale avec statut PENDING
            Payment payment = Payment.builder()
                    .transaction(transaction)
                    .amount(amount)
                    .status(PaymentStatus.PENDING) // En attente du frontend
                    .providerPaymentId(intent.getId()) // ex: pi_3M...
                    .build();

            paymentRepository.save(payment);

            return intent;

        } catch (StripeException e) {
            throw new PaymentFailedException("Erreur lors de l'initialisation Stripe : " + e.getMessage());
        }
    }
}