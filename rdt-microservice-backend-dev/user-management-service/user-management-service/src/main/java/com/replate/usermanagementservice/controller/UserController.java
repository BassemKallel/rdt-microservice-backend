package com.replate.usermanagementservice.controller;

import com.replate.usermanagementservice.dto.*;
import com.replate.usermanagementservice.model.User;
import com.replate.usermanagementservice.model.UserRole;
import com.replate.usermanagementservice.repository.UserRepository;
import com.replate.usermanagementservice.service.UserService;
import com.replate.usermanagementservice.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @GetMapping("/me")
    public ResponseEntity<User> getMyProfile() {
        User user = getCurrentUser();
        return ResponseEntity.ok(user);
    }


    @PutMapping("/me")
    public ResponseEntity<User> updateUser(@RequestBody UpdateProfileRequest user) {
        User existingUser = getCurrentUser();
        User updatedUser = userService.updateUser(existingUser.getId(),user);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/me/password")
    public ResponseEntity<MessageResponse> updateUserPassword(@RequestBody ChangePasswordRequest newPassword) {
        User user = getCurrentUser();

        userService.changePassword(user.getId(), newPassword.getOldPassword(), newPassword.getNewPassword());
        return ResponseEntity.ok(new MessageResponse("Mot de passe modifié avec succès."));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName(); // Le username dans le token est l'email
        return userService.findByEmail(email);
    }


    @GetMapping("{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}