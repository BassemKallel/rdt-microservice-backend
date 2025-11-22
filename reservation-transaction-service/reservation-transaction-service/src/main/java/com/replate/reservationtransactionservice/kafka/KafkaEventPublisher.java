package com.replate.reservationtransactionservice.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventPublisher {

    private static final String RESERVATION_TOPIC = "reservation-events";
    private static final String TRANSACTION_TOPIC = "transaction-events";
    private static final String PAYMENT_TOPIC = "payment-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendReservationCreatedEvent(ReservationCreatedEvent event) {
        kafkaTemplate.send(RESERVATION_TOPIC, "RESERVATION_CREATED", event);
    }

    public void sendTransactionConfirmedEvent(TransactionConfirmedEvent event) {
        kafkaTemplate.send(TRANSACTION_TOPIC, "TRANSACTION_CONFIRMED", event);
    }

    public void sendPaymentSucceededEvent(PaymentSucceededEvent event) {
        kafkaTemplate.send(PAYMENT_TOPIC, "PAYMENT_SUCCEEDED", event);
    }
}
