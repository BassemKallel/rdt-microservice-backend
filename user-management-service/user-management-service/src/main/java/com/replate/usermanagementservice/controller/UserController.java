package com.replate.usermanagementservice.controller;

import com.replate.usermanagementservice.dto.*;
import com.replate.usermanagementservice.model.User;
import com.replate.usermanagementservice.model.UserRole;
import com.replate.usermanagementservice.service.UserService;
import com.replate.usermanagementservice.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Object> registerUser(@Valid @RequestBody RegisterRequest request) {

        // Le GlobalExceptionHandler intercepte les erreurs (EmailExists, MissingFields, IllegalArgument)
        User user = userService.registerNewUser(request);

        if (user.getRole() == UserRole.MERCHANT || user.getRole() == UserRole.ASSOCIATION) {
            return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Inscription réussie. Compte en attente de validation."));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Inscription réussie. Veuillez vous connecter."));
    }

    @PostMapping("/login")
    public ResponseEntity<Object> loginUser(@Valid @RequestBody AuthRequest request) {

        User user = userService.authenticate(request.getEmail(), request.getPassword());

        String token = jwtService.generateToken(user);

        AuthResponse authResponse = new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                token
        );

        return ResponseEntity.ok(authResponse);
    }
    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllAccounts() {
        List<User> allUsers = userService.getAllUsers();
        return ResponseEntity.ok(allUsers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("Utilisateur supprimé avec succès."));
    }
    @PutMapping("/me/profile") // Utilise PUT pour la mise à jour
    public ResponseEntity<User> updateMyProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {

        // Récupère l'email de l'utilisateur authentifié (le Principal)
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();

        // Récupère l'utilisateur complet depuis la base de données
        User user = userService.authenticate(userEmail, null); // Astuce : réutiliser authenticate pour trouver par email

        // Appelle le service pour mettre à jour les données
        User updatedUser = userService.updateUserProfile(user.getId(), request);

        return ResponseEntity.ok(updatedUser);
    }
}