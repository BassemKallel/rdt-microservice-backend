package com.replate.reservationtransactionservice.service;

import com.replate.offermanagementservice.model.*;
import com.replate.reservationtransactionservice.client.AnnouncementClient;
import com.replate.reservationtransactionservice.dto.*;
import com.replate.reservationtransactionservice.exception.PaymentFailedException;
import com.replate.reservationtransactionservice.exception.UnauthorizedActionException;
import com.replate.reservationtransactionservice.model.*;
import com.replate.reservationtransactionservice.repository.PaymentRepository;
import com.replate.reservationtransactionservice.repository.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class ReservationService {

    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final StripePaymentService stripePaymentService;
    private final AnnouncementClient announcementClient;


    public ReservationService(TransactionRepository transactionRepository,
                              PaymentRepository paymentRepository,
                              StripePaymentService stripePaymentService,
                              AnnouncementClient announcementClient) {
        this.transactionRepository = transactionRepository;
        this.paymentRepository = paymentRepository;
        this.stripePaymentService = stripePaymentService;
        this.announcementClient = announcementClient;

    }

    // Create reservation
    public ReservationResponse createReservation(ReservationRequest request) {
        Announcement announcement = announcementClient.getAnnouncementById(request.getAnnonceId());

        if (announcement == null) {
            throw new RuntimeException("Announcement not found");
        }

        if (request.getQuantiteTransmise() == null || request.getQuantiteTransmise() <= 0) {
            throw new RuntimeException("Quantity must be greater than zero.");
        }

        Transaction transaction = Transaction.builder()
                .annonceId(request.getAnnonceId())
                .userId(request.getUserId())
                .merchantId(announcement.getMerchantId())
                .quantiteTransmise(request.getQuantiteTransmise())
                .offerType(request.getOfferType())
                .status(TransactionStatus.PENDING_CONFIRMATION)
                .build();

        transactionRepository.save(transaction);

        return new ReservationResponse(
                transaction.getTransactionId(),
                transaction.getStatus(),
                true,
                request.getAmount(),
                request.getQuantiteTransmise(),
                "Reservation created successfully"
        );
    }

    // Confirm reservation
    public ReservationResponse confirmReservation(ConfirmReservationRequest request) {

        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (request.getMerchantId() != null && !request.getMerchantId().equals(transaction.getMerchantId())) {
            throw new UnauthorizedActionException("You are not allowed to confirm this transaction.");
        }

        // If SALE type → process payment with Stripe
        if (transaction.getOfferType() == OfferType.SALE &&
                transaction.getQuantiteTransmise() != null &&
                transaction.getQuantiteTransmise() > 0 &&
                transaction.getStatus() == TransactionStatus.PENDING_CONFIRMATION) {

            Payment payment = stripePaymentService.processPayment(transaction, transaction.getQuantiteTransmise());
            transaction.setPayment(payment);

            if (payment.getStatus() != PaymentStatus.COMPLETED) {
                transaction.setStatus(TransactionStatus.CANCELLED);
                transactionRepository.save(transaction);
                throw new PaymentFailedException("Payment failed, reservation cancelled.");
            }
        }

        transaction.setStatus(TransactionStatus.CONFIRMED);
        transactionRepository.save(transaction);

        return new ReservationResponse(
                transaction.getTransactionId(),
                transaction.getStatus(),
                true,
                transaction.getQuantiteTransmise(),
                transaction.getQuantiteTransmise(),
                "Reservation confirmed successfully"
        );
    }

    // Reject reservation
    public ReservationResponse rejectReservation(ConfirmReservationRequest request) {

        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (request.getMerchantId() != null && !request.getMerchantId().equals(transaction.getMerchantId())) {
            throw new UnauthorizedActionException("You are not allowed to reject this transaction.");
        }

        if (transaction.getStatus() == TransactionStatus.CONFIRMED ||
                transaction.getStatus() == TransactionStatus.CANCELLED) {
            throw new RuntimeException("This reservation cannot be rejected.");
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        transactionRepository.save(transaction);

        return new ReservationResponse(
                transaction.getTransactionId(),
                transaction.getStatus(),
                true,
                transaction.getQuantiteTransmise(),
                transaction.getQuantiteTransmise(),
                "Reservation rejected successfully"
        );
    }
}
