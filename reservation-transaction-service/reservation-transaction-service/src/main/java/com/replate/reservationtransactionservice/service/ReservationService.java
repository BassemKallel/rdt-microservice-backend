package com.replate.reservationtransactionservice.service;

import com.replate.reservationtransactionservice.client.AnnouncementClient;
import com.replate.reservationtransactionservice.dto.*;
import com.replate.reservationtransactionservice.exception.*;
import com.replate.reservationtransactionservice.model.*;
import com.replate.reservationtransactionservice.repository.TransactionRepository;
import com.stripe.model.PaymentIntent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
    @CircuitBreaker(name = "externalService", fallbackMethod = "fallbackCreateReservation")
    public ReservationResponse createReservation(ReservationRequest request) {
        // Récupération de l'annonce via OMS
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

        return mapToResponse(transaction);
    }

    public ReservationResponse fallbackCreateReservation(ReservationRequest request, Exception e) {
        // Log l'erreur pour le débogage
        // log.error("Fallback pour createReservation activé; error: {}", e.getMessage());

        // Créer une réponse d'erreur standardisée
        ReservationResponse response = new ReservationResponse();
        response.setMessage("Service indisponible pour le moment. Veuillez réessayer plus tard.");
        // Vous pouvez définir d'autres champs sur un état par défaut si nécessaire
        response.setStatus(TransactionStatus.CANCELLED); // Ou un autre statut pertinent
        return response;
    }

    // --- 2. CONFIRMATION (Marchand) ---
    @Transactional
    public ReservationResponse confirmReservation(ConfirmReservationRequest request) {

        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée"));

        if (request.getMerchantId() != null && !request.getMerchantId().equals(transaction.getMerchantId())) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à confirmer cette transaction.");
        }

        if (transaction.getStatus() == TransactionStatus.CONFIRMED) {
            return mapToResponse(transaction);
        }

        // 🟢 DÉCRÉMENTATION DU STOCK (Appel Synchrone OMS)
        try {
            Integer qtyToDecrease = Math.round(transaction.getQuantiteTransmise());
            announcementClient.decreaseStock(transaction.getAnnonceId(), qtyToDecrease);
        } catch (Exception e) {
            throw new BusinessException("Impossible de confirmer : Stock insuffisant ou erreur service offre.");
        }

        String clientSecret = null;
        String message = "Réservation confirmée";

        if (transaction.getOfferType() == OfferType.SALE) {
            AnnouncementResponse announcement = announcementClient.getAnnouncementById(transaction.getAnnonceId());
            if (announcement == null) {
                throw new ResourceNotFoundException("Annonce introuvable.");
            }
            transaction.setStatus(TransactionStatus.PENDING_PAYMENT);
            Float totalAmount = (float) (announcement.getPrice() * transaction.getQuantiteTransmise());

            PaymentIntent intent = stripePaymentService.createPaymentIntent(transaction, totalAmount);
            clientSecret = intent.getClientSecret();
            message = "Paiement requis. Utilisez le clientSecret pour finaliser.";

            // Le statut reste PENDING_CONFIRMATION jusqu'au Webhook Stripe

        } else {
            transaction.setStatus(TransactionStatus.CONFIRMED);
            transactionRepository.save(transaction);
        }

        // On construit la réponse manuellement ici pour inclure le clientSecret fraîchement généré
        ReservationResponse response = mapToResponse(transaction);
        response.setMessage(message);
        response.setPaymentClientSecret(clientSecret);
        return response;
    }

    // --- 3. REJET (Marchand) ---
    @Transactional
    public ReservationResponse rejectReservation(ConfirmReservationRequest request) {
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée"));

        if (request.getMerchantId() != null && !request.getMerchantId().equals(transaction.getMerchantId())) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à rejeter cette transaction.");
        }

        if (transaction.getStatus() != TransactionStatus.PENDING_CONFIRMATION) {
            throw new BusinessException("Cette réservation ne peut plus être rejetée.");
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        transactionRepository.save(transaction);

        return mapToResponse(transaction);
    }

    // --- 4. DÉTAIL (Polling Client) ---
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée"));
        return mapToResponse(transaction);
    }

    // --- 5. HISTORIQUE UTILISATEUR (Client ou Marchand) ---
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByUser(Long userId, String userRole) {
        List<Transaction> transactions;
        String role = userRole.replace("ROLE_", "").trim().toUpperCase();

        if ("MERCHANT".equals(role)) {
            transactions = transactionRepository.findByMerchantId(userId);
        } else {
            transactions = transactionRepository.findByUserId(userId);
        }
        return transactions.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllReservations() {
        return transactionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ReservationResponse mapToResponse(Transaction transaction) {
        String clientSecret = null;
        String message = "Statut: " + transaction.getStatus();

        if (transaction.getOfferType() == OfferType.SALE
                && transaction.getPayment() != null
                && transaction.getPayment().getStatus() == PaymentStatus.PENDING) {
            clientSecret = stripePaymentService.getClientSecret(transaction.getPayment().getProviderPaymentId());
            message = "Paiement requis";
        }

        Float price = 0.0f;
        if (transaction.getPayment() != null) {
            price = transaction.getPayment().getAmount();
        } else if (transaction.getOfferType() == OfferType.SALE) {
            // Prix estimé (si pas encore de paiement)
            price = 0.0f;
        }

        String title = "Annonce #" + transaction.getAnnonceId();
        try {
            AnnouncementResponse ann = announcementClient.getAnnouncementById(transaction.getAnnonceId());
            if (ann != null && ann.getTitle() != null) {
                title = ann.getTitle();
            }
        } catch (Exception e) {
            // Ignorer les erreurs Feign pour ne pas bloquer l'affichage de la liste
        }

        return ReservationResponse.builder()
                .transactionId(transaction.getTransactionId())
                .status(transaction.getStatus())
                .active(transaction.getStatus() != TransactionStatus.CANCELLED)
                .availableQuantity(transaction.getQuantiteTransmise())
                .price(price)
                .message(message)
                .paymentClientSecret(clientSecret)
                .transactionDate(transaction.getTransactionDate())
                .userId(transaction.getUserId())
                .announcementId(transaction.getAnnonceId())
                .announcementTitle(title)
                .build();
    }

    @Transactional
    public ReservationResponse markAsDelivered(Long transactionId, Long userId ) {
        Transaction transaction = transactionRepository.findById(transactionId).
                orElseThrow(()->new ResourceNotFoundException("Transaction not found"));
        if (!transaction.getMerchantId().equals(userId)) {
            throw new UnauthorizedActionException("Seul le marchand peut marquer la commande comme livrée.");
        }
        if(transaction.getStatus() != TransactionStatus.CONFIRMED){
            throw new UnauthorizedActionException("Impossible de livrer : La réservation n'est pas encore confirmée ou est déjà terminée.");
        }
        transaction.setStatus(TransactionStatus.DELIVERED);
        transactionRepository.save(transaction);
        return mapToResponse(transaction);
    }
}