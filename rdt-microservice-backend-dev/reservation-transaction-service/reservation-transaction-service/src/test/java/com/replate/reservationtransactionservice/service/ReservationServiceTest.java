package com.replate.reservationtransactionservice.service;

import com.replate.reservationtransactionservice.client.AnnouncementClient;
import com.replate.reservationtransactionservice.dto.AnnouncementResponse;
import com.replate.reservationtransactionservice.dto.ConfirmReservationRequest;
import com.replate.reservationtransactionservice.dto.ReservationRequest;
import com.replate.reservationtransactionservice.dto.ReservationResponse;
import com.replate.reservationtransactionservice.exception.ResourceNotFoundException;
import com.replate.reservationtransactionservice.exception.UnauthorizedActionException;
import com.replate.reservationtransactionservice.model.OfferType;
import com.replate.reservationtransactionservice.model.Transaction;
import com.replate.reservationtransactionservice.model.TransactionStatus;
import com.replate.reservationtransactionservice.repository.PaymentRepository;
import com.replate.reservationtransactionservice.repository.TransactionRepository;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private StripePaymentService stripePaymentService;
    @Mock
    private AnnouncementClient announcementClient;

    @InjectMocks
    private ReservationService reservationService;

    private Transaction transaction;
    private AnnouncementResponse announcementResponse;

    @BeforeEach
    void setUp() {
        transaction = Transaction.builder()
                .transactionId(1L)
                .annonceId(100L)
                .userId(50L)
                .merchantId(10L)
                .quantiteTransmise(2.0f)
                .offerType(OfferType.SALE)
                .status(TransactionStatus.PENDING_CONFIRMATION)
                .build();

        announcementResponse = AnnouncementResponse.builder()
                .id(100L)
                .merchantId(10L)
                .title("Test Annonce")
                .price(10.0)
                .build();
    }

    @Test
    void createReservation_Success() {
        ReservationRequest request = new ReservationRequest(100L, 50L, 2.0f, OfferType.SALE, 20.0f);
        when(announcementClient.getAnnouncementById(100L)).thenReturn(announcementResponse);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction t = i.getArgument(0);
            t.setTransactionId(1L);
            return t;
        });

        ReservationResponse response = reservationService.createReservation(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.PENDING_CONFIRMATION, response.getStatus());
        assertEquals(20.0f, response.getPrice());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createReservation_AnnouncementNotFound() {
        ReservationRequest request = new ReservationRequest(999L, 50L, 1.0f, OfferType.SALE, 10.0f);
        when(announcementClient.getAnnouncementById(999L)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> reservationService.createReservation(request));
    }

    @Test
    void confirmReservation_Sale_ShouldCallStripe() {
        ConfirmReservationRequest request = new ConfirmReservationRequest(1L, 10L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(announcementClient.getAnnouncementById(100L)).thenReturn(announcementResponse);

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getClientSecret()).thenReturn("pi_secret_test_123");

        when(stripePaymentService.createPaymentIntent(any(Transaction.class), eq(20.0f)))
                .thenReturn(mockIntent);

        ReservationResponse response = reservationService.confirmReservation(request);

        assertEquals("pi_secret_test_123", response.getPaymentClientSecret());
        assertEquals("Paiement requis. Utilisez le clientSecret pour finaliser.", response.getMessage());
        assertEquals(TransactionStatus.PENDING_CONFIRMATION, response.getStatus());
    }

    @Test
    void confirmReservation_Donation_ShouldConfirmImmediately() {
        transaction.setOfferType(OfferType.DONATION);
        ConfirmReservationRequest request = new ConfirmReservationRequest(1L, 10L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        ReservationResponse response = reservationService.confirmReservation(request);

        assertEquals(TransactionStatus.CONFIRMED, response.getStatus());
        assertNull(response.getPaymentClientSecret());
        verify(stripePaymentService, never()).createPaymentIntent(any(), any());
        verify(transactionRepository).save(transaction);
    }

    @Test
    void confirmReservation_UnauthorizedMerchant() {
        ConfirmReservationRequest request = new ConfirmReservationRequest(1L, 999L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        assertThrows(UnauthorizedActionException.class, () -> reservationService.confirmReservation(request));
    }
}