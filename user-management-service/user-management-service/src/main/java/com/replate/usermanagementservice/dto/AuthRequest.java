package com.replate.usermanagementservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {

    @NotBlank(message = "L'email est requis.")
    private String email;
    @NotBlank(message = "Le mot de passe est requis")
    private String password;
}