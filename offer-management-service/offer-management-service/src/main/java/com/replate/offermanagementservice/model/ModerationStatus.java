package com.replate.offermanagementservice.model;

public enum ModerationStatus {
    PENDING_REVIEW,  // En attente de revue (par défaut à la création)
    ACCEPTED,        // Accepté par la modération (visible)
    REJECTED,        // Rejeté par la modération
    ARCHIVED         // Retiré ou expiré
}