package com.replate.reservationtransactionservice.service;

import com.replate.reservationtransactionservice.client.AnnouncementClient;
import com.replate.reservationtransactionservice.dto.*;
import com.replate.reservationtransactionservice.exception.*;
import com.replate.reservationtransactionservice.model.*;
import com.replate.reservationtransactionservice.repository.TransactionRepository;
import com.stripe.model.PaymentIntent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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

    // --- 2. CONFIRMATION (MISE À JOUR AVEC STOCK) ---
    @Transactional
    public ReservationResponse confirmReservation(ConfirmReservationRequest request) {

        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée"));

        // Vérification que c'est bien le marchand qui confirme
        if (request.getMerchantId() != null && !request.getMerchantId().equals(transaction.getMerchantId())) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à confirmer cette transaction.");
        }

        // 🟢 DÉCRÉMENTATION DU STOCK (Appel Synchrone OMS)
        try {
            // On cast en Integer car le stock est un int, mais la quantité transaction est Float
            Integer qtyToDecrease = Math.round(transaction.getQuantiteTransmise());
            announcementClient.decreaseStock(transaction.getAnnonceId(), qtyToDecrease);
        } catch (Exception e) {
            // On capture l'erreur (400 Bad Request de l'OMS si stock insuffisant)
            throw new BusinessException("Impossible de confirmer : Stock insuffisant ou erreur service offre.");
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
                .paymentClientSecret(clientSecret)
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

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée"));

        String clientSecret = null;
        String message = "En attente";

        // Si une vente est confirmée mais pas encore payée, on récupère le secret
        if (transaction.getOfferType() == OfferType.SALE
                && transaction.getPayment() != null
                && transaction.getPayment().getStatus() == PaymentStatus.PENDING) {

            // On appelle Stripe pour récupérer le secret à jour
            clientSecret = stripePaymentService.getClientSecret(transaction.getPayment().getProviderPaymentId());
            message = "Paiement requis";
        } else if (transaction.getStatus() == TransactionStatus.CONFIRMED) {
            message = "Réservation validée et payée";
        }

        // Calcul du prix (si vente)
        // Attention: Pour être précis, il faudrait stocker le prix figé dans la transaction.
        // Ici on fait une estimation simple ou on met 0.
        Float price = 0.0f;
        if(transaction.getPayment() != null) {
            price = transaction.getPayment().getAmount();
        }

        return ReservationResponse.builder()
                .transactionId(transaction.getTransactionId())
                .status(transaction.getStatus())
                .active(transaction.getStatus() != TransactionStatus.CANCELLED)
                .availableQuantity(transaction.getQuantiteTransmise())
                .price(price)
                .message(message)
                .paymentClientSecret(clientSecret) // C'est ce que le Client attend !
                .build();
    }
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByUser(Long userId, String userRole) {
        List<Transaction> transactions;

        // Nettoyage du rôle (au cas où il arrive sous la forme "ROLE_MERCHANT")
        String role = userRole.startsWith("ROLE_") ? userRole.substring(5) : userRole;

        if ("MERCHANT".equalsIgnoreCase(role)) {
            // Cas MARCHAND : Il voit toutes les ventes de SES produits
            transactions = transactionRepository.findByMerchantId(userId);
        } else {
            // Cas CLIENT (Individual/Association) : Il voit SES achats
            transactions = transactionRepository.findByUserId(userId);
        }

        // On transforme chaque Transaction en ReservationResponse
        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- 2. MÉTHODE UTILITAIRE : MAPPING ---
    private ReservationResponse mapToResponse(Transaction transaction) {
        String clientSecret = null;
        String message = "Statut: " + transaction.getStatus();

        // Logique pour récupérer le secret Stripe (seulement si c'est une vente non payée)
        if (transaction.getOfferType() == OfferType.SALE
                && transaction.getPayment() != null
                && transaction.getPayment().getStatus() == PaymentStatus.PENDING) {
            // Optionnel : Pour une liste, on peut éviter cet appel pour la performance,
            // mais c'est utile si le client clique sur "Payer" depuis l'historique.
            clientSecret = stripePaymentService.getClientSecret(transaction.getPayment().getProviderPaymentId());
        }

        // Calcul du prix à afficher
        Float price = 0.0f;
        if (transaction.getPayment() != null) {
            price = transaction.getPayment().getAmount();
        } else if (transaction.getOfferType() == OfferType.SALE) {
            // Si pas encore de paiement (ex: PENDING_CONFIRMATION), on estime le prix
            // Note: Idéalement, stockez le prix unitaire dans Transaction pour figer le prix
            price = 0.0f;
        }

        // Récupération du Titre de l'annonce via Feign (Appel à OMS)
        String title = "Annonce #" + transaction.getAnnonceId();
        try {
            AnnouncementResponse ann = announcementClient.getAnnouncementById(transaction.getAnnonceId());
            if (ann != null && ann.getTitle() != null) {
                title = ann.getTitle();
            }
        } catch (Exception e) {
            // Si l'annonce a été supprimée ou le service injoignable, on garde l'ID par défaut
            System.err.println("Impossible de récupérer le titre pour l'annonce " + transaction.getAnnonceId());
        }

        return ReservationResponse.builder()
                .transactionId(transaction.getTransactionId())
                .status(transaction.getStatus())
                .active(transaction.getStatus() != TransactionStatus.CANCELLED)
                .availableQuantity(transaction.getQuantiteTransmise())
                .price(price)
                .message(message)
                .paymentClientSecret(clientSecret)
                // Nouveaux champs
                .transactionDate(transaction.getTransactionDate())
                .userId(transaction.getUserId())
                .announcementId(transaction.getAnnonceId())
                .announcementTitle(title)
                .build();
    }
}