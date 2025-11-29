package com.replate.reservationtransactionservice.model;

public enum TransactionStatus {
    PENDING_CONFIRMATION,
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    DELIVERED
}
