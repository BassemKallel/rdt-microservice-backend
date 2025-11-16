package com.replate.usermanagementservice.controller;

import com.replate.usermanagementservice.dto.AuthRequest;
import com.replate.usermanagementservice.dto.AuthResponse;
import com.replate.usermanagementservice.dto.MessageResponse;
import com.replate.usermanagementservice.dto.RegisterRequest;
import com.replate.usermanagementservice.model.User;
import com.replate.usermanagementservice.model.UserRole;
import com.replate.usermanagementservice.service.UserService;
import com.replate.usermanagementservice.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}