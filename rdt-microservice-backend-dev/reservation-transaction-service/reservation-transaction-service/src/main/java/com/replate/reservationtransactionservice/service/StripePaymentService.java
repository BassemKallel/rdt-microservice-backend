package com.replate.reservationtransactionservice.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.replate.reservationtransactionservice.model.*;
import com.replate.reservationtransactionservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    public StripePaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment processPayment(Transaction transaction, Float amount) {

        com.stripe.Stripe.apiKey = stripeApiKey;

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long)(amount * 100)) // Stripe expects cents
                .setCurrency("tnd")
                .build();

        try {
            PaymentIntent intent = PaymentIntent.create(params);

            Payment payment = Payment.builder()
                    .transaction(transaction)
                    .amount(amount)
                    .status(PaymentStatus.COMPLETED) // assume payment succeeds immediately
                    .providerPaymentId(intent.getId())
                    .build();

            return paymentRepository.save(payment);

        } catch (StripeException e) {
            Payment payment = Payment.builder()
                    .transaction(transaction)
                    .amount(amount)
                    .status(PaymentStatus.REFUNDED)
                    .providerPaymentId(null)
                    .build();
            paymentRepository.save(payment);
            return payment;
        }
    }
}
