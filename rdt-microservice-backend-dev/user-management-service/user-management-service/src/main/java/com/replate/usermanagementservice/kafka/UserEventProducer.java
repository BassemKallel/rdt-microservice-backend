package com.replate.usermanagementservice.kafka;

import com.replate.usermanagementservice.model.User;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserEventProducer {

    private static final String USER_TOPIC = "user-events";
    private final KafkaTemplate<String, User> kafkaTemplate;

    public UserEventProducer(KafkaTemplate<String, User> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendUserRegisteredEvent(User user) {
        kafkaTemplate.send(USER_TOPIC, "USER_REGISTERED", user);
    }
}
