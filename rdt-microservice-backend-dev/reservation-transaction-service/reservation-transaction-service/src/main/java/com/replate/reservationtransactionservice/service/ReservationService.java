package com.replate.reservationtransactionservice.service;

import com.replate.reservationtransactionservice.client.AnnouncementClient;
import com.replate.reservationtransactionservice.dto.*;
import com.replate.reservationtransactionservice.exception.*;
import com.replate.reservationtransactionservice.model.*;
import com.replate.reservationtransactionservice.repository.TransactionRepository;
import com.stripe.model.PaymentIntent; // ✅ Import indispensable pour Stripe
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final TransactionRepository transactionRepository;
    private final StripePaymentService stripePaymentService;
    private final AnnouncementClient announcementClient;

    public ReservationService(TransactionRepository transactionRepository,
                              StripePaymentService stripePaymentService,
                              AnnouncementClient announcementClient) {
        this.transactionRepository = transactionRepository;
        this.stripePaymentService = stripePaymentService;
        this.announcementClient = announcementClient;
    }

    // --- 1. CRÉATION ---
    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        // Récupération de l'annonce
        AnnouncementResponse announcement = announcementClient.getAnnouncementById(request.getAnnonceId());

        if (announcement == null) {
            throw new ResourceNotFoundException("Annonce non trouvée pour l'ID: " + request.getAnnonceId());
        }

        if (request.getQuantiteTransmise() == null || request.getQuantiteTransmise() <= 0) {
            throw new BusinessException("La quantité doit être supérieure à zéro.");
        }

        // Création de la transaction en base
        Transaction transaction = Transaction.builder()
                .annonceId(request.getAnnonceId())
                .userId(request.getUserId())
                .merchantId(announcement.getMerchantId())
                .quantiteTransmise(request.getQuantiteTransmise())
                .offerType(request.getOfferType())
                .status(TransactionStatus.PENDING_CONFIRMATION)
                .build();

        transactionRepository.save(transaction);

        return ReservationResponse.builder()
                .transactionId(transaction.getTransactionId())
                .status(transaction.getStatus())
                .active(true)
                .price(request.getAmount())
                .availableQuantity(request.getQuantiteTransmise())
                .message("Réservation créée avec succès")
                .build();
    }

    // --- 2. CONFIRMATION (Avec Paiement Stripe) ---
    @Transactional
    public ReservationResponse confirmReservation(ConfirmReservationRequest request) {

        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée"));

        // Vérification que c'est bien le marchand qui confirme
        if (request.getMerchantId() != null && !request.getMerchantId().equals(transaction.getMerchantId())) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à confirmer cette transaction.");
        }

        String clientSecret = null;
        String message = "Réservation confirmée";

        // LOGIQUE VENTE : On déclenche le paiement Stripe
        if (transaction.getOfferType() == OfferType.SALE) {

            // Si déjà confirmé, on ne fait rien
            if (transaction.getStatus() == TransactionStatus.CONFIRMED) {
                return ReservationResponse.builder()
                        .transactionId(transaction.getTransactionId())
                        .status(transaction.getStatus())
                        .message("Déjà confirmée")
                        .build();
            }

            // 1. Récupérer le prix unitaire de l'annonce pour calculer le total
            AnnouncementResponse announcement = announcementClient.getAnnouncementById(transaction.getAnnonceId());
            if (announcement == null) {
                throw new ResourceNotFoundException("Impossible de récupérer l'annonce pour le calcul du prix.");
            }

            // Calcul du prix total (Prix * Quantité)
            Float totalAmount = (float) (announcement.getPrice() * transaction.getQuantiteTransmise());

            // 2. Appeler Stripe pour créer l'intention (PaymentIntent)
            // ✅ C'est ici qu'on appelle la NOUVELLE méthode createPaymentIntent
            PaymentIntent intent = stripePaymentService.createPaymentIntent(transaction, totalAmount);

            // 3. Récupérer le secret pour le frontend
            clientSecret = intent.getClientSecret();
            message = "Paiement requis. Utilisez le clientSecret pour finaliser.";

            // On laisse le statut en PENDING_CONFIRMATION en attendant le Webhook

        } else {
            // LOGIQUE DONATION : Confirmation immédiate
            transaction.setStatus(TransactionStatus.CONFIRMED);
            transactionRepository.save(transaction);
        }

        return ReservationResponse.builder()
                .transactionId(transaction.getTransactionId())
                .status(transaction.getStatus())
                .active(true)
                .availableQuantity(transaction.getQuantiteTransmise())
                .message(message)
                .paymentClientSecret(clientSecret) // ✅ Le secret est envoyé ici
                .build();
    }

    // --- 3. REJET ---
    @Transactional
    public ReservationResponse rejectReservation(ConfirmReservationRequest request) {
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée"));

        if (request.getMerchantId() != null && !request.getMerchantId().equals(transaction.getMerchantId())) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à rejeter cette transaction.");
        }

        if (transaction.getStatus() == TransactionStatus.CONFIRMED ||
                transaction.getStatus() == TransactionStatus.CANCELLED) {
            throw new BusinessException("Cette réservation est déjà traitée et ne peut être rejetée.");
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        transactionRepository.save(transaction);

        return ReservationResponse.builder()
                .transactionId(transaction.getTransactionId())
                .status(transaction.getStatus())
                .active(false)
                .availableQuantity(transaction.getQuantiteTransmise())
                .message("Réservation rejetée avec succès")
                .build();
    }
}