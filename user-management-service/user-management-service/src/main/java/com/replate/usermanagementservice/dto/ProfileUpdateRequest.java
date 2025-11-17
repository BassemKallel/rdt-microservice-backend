package com.replate.usermanagementservice.dto;

import lombok.Data;

// DTO pour la mise à jour du profil
@Data
public class ProfileUpdateRequest {
    // Les seuls champs modifiables par l'utilisateur
    private String username;
    private String phoneNumber;
    private String location;
}