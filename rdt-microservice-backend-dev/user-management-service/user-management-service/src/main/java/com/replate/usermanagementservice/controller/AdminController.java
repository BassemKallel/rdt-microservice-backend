package com.replate.usermanagementservice.controller;

import com.replate.usermanagementservice.dto.MessageResponse;
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


    @PostMapping("/validate/{id}")
    public ResponseEntity<?> validateAccount(@PathVariable Long id) {
        User validatedUser = userService.validateUser(id);
        return ResponseEntity.ok("Le compte de " + validatedUser.getEmail() + " a été validé.");
    }


    @GetMapping("/pending")
    public ResponseEntity<List<User>> getPendingAccounts() {
        List<User> pendingUsers = userService.getPendingValidationAccounts();
        return ResponseEntity.ok(pendingUsers);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteAccount(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully."));
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/rejected")
    public ResponseEntity<List<User>> getRejectedUsers() {
        return ResponseEntity.ok(userService.getRejectedUsers());
    }

    @GetMapping("/suspended")
    public ResponseEntity<List<User>> getSuspendedUsers() {
        return ResponseEntity.ok(userService.getSuspendedUsers());
    }
}