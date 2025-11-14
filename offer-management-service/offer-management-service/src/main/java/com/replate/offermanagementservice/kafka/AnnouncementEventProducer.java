package com.replate.offermanagementservice.kafka;

import com.replate.offermanagementservice.model.Announcement;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementEventProducer {
    private static final String OFFER_TOPIC = "offer-events";
    private final KafkaTemplate<String, Announcement> kafkaTemplate;

    public AnnouncementEventProducer(KafkaTemplate<String, Announcement> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendAnnouncementEvent(Announcement announcement, String eventType) {
        // Envoie le message sur le topic 'offer-events' avec le type d'événement (AD_CREATED, AD_UPDATED, etc.)
        kafkaTemplate.send(OFFER_TOPIC, eventType, announcement);
        System.out.println("✅ Événement Kafka : " + eventType + " envoyé pour Annonce ID: " + announcement.getId());
    }
}