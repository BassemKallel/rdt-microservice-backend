package com.replate.reservationtransactionservice.repository;

import com.replate.reservationtransactionservice.model.Transaction;
import com.replate.reservationtransactionservice.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserId(Long userId);

    // All reservations for an announcement
    List<Transaction> findByAnnonceId(Long annonceId);

    // For logic: check if the user already claimed the same announcement
    boolean existsByUserIdAndAnnonceId(Long userId, Long annonceId);

    // Find by merchant
    List<Transaction> findByMerchantId(Long merchantId);

    // Find by transaction status (pending, confirmed, cancelled)
    List<Transaction> findByStatus(TransactionStatus status);
}