package com.replate.reservationtransactionservice.controller;

import com.replate.reservationtransactionservice.dto.ConfirmReservationRequest;
import com.replate.reservationtransactionservice.dto.ReservationRequest;
import com.replate.reservationtransactionservice.dto.ReservationResponse;
import com.replate.reservationtransactionservice.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // Créer une réservation (Statut PENDING)
    @PostMapping("/create")
    public ResponseEntity<ReservationResponse> createReservation(
            @RequestBody ReservationRequest request) {

        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.ok(response);
    }

    // Confirmer une réservation (Déclenche le paiement si nécessaire)
    @PostMapping("/confirm")
    public ResponseEntity<ReservationResponse> confirmReservation(
            @RequestBody ConfirmReservationRequest request) {

        ReservationResponse response = reservationService.confirmReservation(request);
        return ResponseEntity.ok(response);
    }

    // Rejeter une réservation
    @PostMapping("/reject")
    public ResponseEntity<ReservationResponse> rejectReservation(
            @RequestBody ConfirmReservationRequest request) {

        ReservationResponse response = reservationService.rejectReservation(request);
        return ResponseEntity.ok(response);
    }
}