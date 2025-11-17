package com.replate.usermanagementservice.controller;

import com.replate.usermanagementservice.model.User;
import com.replate.usermanagementservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Valide un compte utilisateur (Merchant ou Association).
     * Le GlobalExceptionHandler interceptera une ResourceNotFoundException si l'ID est invalide.
     */
    @PostMapping("/validate/{id}")
    public ResponseEntity<?> validateAccount(@PathVariable Long id) {
        // CORRECTION : Appeler le service ET envelopper la réponse
        User validatedUser = userService.validateUser(id);

        // Renvoyer un message de succès
        return ResponseEntity.ok("Le compte de " + validatedUser.getEmail() + " a été validé.");
    }

    /**
     * Récupère les comptes en attente de validation.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<User>> getPendingAccounts() {
        List<User> pendingUsers = userService.getPendingValidationAccounts();
        return ResponseEntity.ok(pendingUsers);
    }
}