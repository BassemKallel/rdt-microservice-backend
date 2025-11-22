package com.replate.usermanagementservice.model;

public enum AccountStatus {
    PENDING,    // En attente de validation (défaut à l'inscription)
    ACTIVE,     // Validé par l'admin
    REJECTED,   // Refusé lors de l'inscription
    SUSPENDED   // Invalidé après avoir été actif (banni)
}