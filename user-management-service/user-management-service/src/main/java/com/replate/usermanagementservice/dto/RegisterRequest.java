package com.replate.usermanagementservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "L'email est requis.")
    private String email;
    @NotBlank(message = "Le mot de passe est requis")
    private String password;
    private String role;
    private String username;
    private String phoneNumber;
    private String location;
    private String documentUrl;
}