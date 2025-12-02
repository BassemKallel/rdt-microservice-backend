package com.replate.reservationtransactionservice.scheduler;

import com.replate.reservationtransactionservice.client.AnnouncementClient;
import com.replate.reservationtransactionservice.model.Transaction;
import com.replate.reservationtransactionservice.model.TransactionStatus;
import com.replate.reservationtransactionservice.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReservationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationCleanupScheduler.class);
    private final TransactionRepository transactionRepository;
    private final AnnouncementClient announcementClient;

    public ReservationCleanupScheduler(TransactionRepository transactionRepository, AnnouncementClient announcementClient) {
        this.transactionRepository = transactionRepository;
        this.announcementClient = announcementClient;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredReservations() {
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(15);
        List<Transaction> pendingTransactions = transactionRepository.findByStatus(TransactionStatus.PENDING_PAYMENT);
        List<Transaction> expiredTransactions = pendingTransactions.stream()
                .filter(t -> t.getTransactionDate().isBefore(expirationTime))
                .toList();
        if (!expiredTransactions.isEmpty()) {
            log.info("Nettoyage : {} transactions expirées trouvées.", expiredTransactions.size());
        }
        for (Transaction tx : expiredTransactions) {
            try {
                log.info("Annulation automatique de la transaction expirée #{}", tx.getTransactionId());
                Integer qtyToRestore = Math.round(tx.getQuantiteTransmise());
                announcementClient.increaseStock(tx.getAnnonceId(), qtyToRestore);
                tx.setStatus(TransactionStatus.CANCELLED);
                transactionRepository.save(tx);
            } catch (Exception e) {
                log.error("Erreur critique lors de la compensation de la transaction #{}", tx.getTransactionId(), e);
            }
        }
    }
}