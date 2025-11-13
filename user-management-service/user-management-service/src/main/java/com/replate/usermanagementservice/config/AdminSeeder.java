package com.replate.usermanagementservice.config;

import com.replate.usermanagementservice.dto.RegisterRequest;
import com.replate.usermanagementservice.model.UserRole;
import com.replate.usermanagementservice.repository.UserRepository;
import com.replate.usermanagementservice.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    public AdminSeeder(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@replate.com";

        if (!userRepository.existsByEmail(adminEmail)) {

            System.out.println("--- Création du compte Administrateur par défaut ---");

            RegisterRequest adminRequest = new RegisterRequest();
            adminRequest.setEmail(adminEmail);
            adminRequest.setPassword("admin12345");
            adminRequest.setRole("ADMIN");

            try {
                userService.registerNewUser(adminRequest);
                System.out.println("Compte Admin créé avec succès.");

            } catch (Exception e) {
                System.err.println("Erreur lors de la création de l'admin: " + e.getMessage());
            }
        } else {
            System.out.println("Compte Admin déjà initialisé.");
        }
    }
}