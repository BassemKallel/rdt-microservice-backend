package com.replate.reportingsafetyservice.kafka;

import com.replate.reportingsafetyservice.dto.UserEventDTO; // Assurez-vous d'avoir créé ce DTO miroir
import com.replate.reportingsafetyservice.service.EmailService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventConsumer {

    private final EmailService emailService;

    public UserEventConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "user-events", groupId = "reporting-group")
    public void consumeUserEvent(ConsumerRecord<String, UserEventDTO> record) {
        String eventType = (String) record.key();
        UserEventDTO user = record.value();

        if (user == null) return;

        switch (eventType) {
            case "USER_REGISTERED":
                handleRegistration(user);
                break;
            case "USER_VALIDATED":
                handleValidation(user);
                break;
            default:
                System.out.println("Event ignoré : " + eventType);
        }
    }

    private void handleRegistration(UserEventDTO user) {
        // Mail de Bienvenue pour INDIVIDUAL uniquement (les autres sont en attente)
        if ("INDIVIDUAL".equalsIgnoreCase(user.getRole())) {
            String subject = "Bienvenue sur Replate !";
            String body = "Bonjour " + user.getUsername() + ",\n\n" +
                    "Votre compte a été créé avec succès. Vous pouvez dès maintenant réserver des paniers !";
            emailService.sendEmail(user.getEmail(), subject, body);
        } else {
            // Optionnel : Mail "En attente de validation" pour Merchant/Assoc
            String subject = "Inscription reçue - En attente de validation";
            String body = "Bonjour " + user.getUsername() + ",\n\n" +
                    "Votre compte est en cours de vérification par nos administrateurs.";
            emailService.sendEmail(user.getEmail(), subject, body);
        }
    }

    private void handleValidation(UserEventDTO user) {
        // Mail envoyé UNIQUEMENT quand l'admin valide (Merchant/Assoc)
        String subject = "Compte Validé !";
        String body = "Félicitations " + user.getUsername() + ",\n\n" +
                "Votre compte " + user.getRole() + " a été validé par un administrateur.\n" +
                "Vous pouvez maintenant publier des annonces.";

        emailService.sendEmail(user.getEmail(), subject, body);
    }
}