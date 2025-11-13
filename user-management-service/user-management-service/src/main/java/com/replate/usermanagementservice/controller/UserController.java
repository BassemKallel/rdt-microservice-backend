package com.replate.usermanagementservice.controller;

import com.replate.usermanagementservice.dto.AuthRequest;
import com.replate.usermanagementservice.dto.AuthResponse;
import com.replate.usermanagementservice.dto.MessageResponse;
import com.replate.usermanagementservice.dto.RegisterRequest;
import com.replate.usermanagementservice.model.User;
import com.replate.usermanagementservice.model.UserRole;
import com.replate.usermanagementservice.service.UserService;
import com.replate.usermanagementservice.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Object> registerUser(@RequestBody RegisterRequest request) {

        // Le GlobalExceptionHandler intercepte les erreurs (EmailExists, MissingFields, IllegalArgument)
        User user = userService.registerNewUser(request);

        // Renvoyer le message approprié en fonction du rôle
        if (user.getRole() == UserRole.MERCHANT || user.getRole() == UserRole.ASSOCIATION) {
            return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Inscription réussie. Compte en attente de validation."));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Inscription réussie. Veuillez vous connecter."));
    }

    @PostMapping("/login")
    public ResponseEntity<Object> loginUser(@RequestBody AuthRequest request) {

        // Le GlobalExceptionHandler intercepte (ResourceNotFoundException, InvalidCredentialsException)
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
}