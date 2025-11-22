package com.replate.reservationtransactionservice.repository;

import com.replate.reservationtransactionservice.model.Transaction;
import com.replate.reservationtransactionservice.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>{
    // Find payment by its transaction
    Optional<Payment> findByTransaction_TransactionId(Long transactionId);

    // Find payment by provider gateway ID
    Optional<Payment> findByProviderPaymentId(String providerPaymentId);
}
