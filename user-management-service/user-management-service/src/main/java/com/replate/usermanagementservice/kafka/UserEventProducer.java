package com.replate.usermanagementservice.kafka;

import com.replate.usermanagementservice.dto.UserEventDTO;
import com.replate.usermanagementservice.model.User;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserEventProducer {

    private static final String USER_TOPIC = "user-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private UserEventDTO mapToDto(User user) {
        return UserEventDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    public void sendUserRegisteredEvent(User user) {
        UserEventDTO event = mapToDto(user); // <-- Conversion obligatoire
        kafkaTemplate.send(USER_TOPIC, "USER_REGISTERED", event); // <-- Envoi du DTO
    }

    public void sendUserValidatedEvent(User user) {
        UserEventDTO event = mapToDto(user);
        kafkaTemplate.send(USER_TOPIC, "USER_VALIDATED", event);
        System.out.println("📨 Event USER_VALIDATED envoyé pour : " + user.getEmail());
    }
}
