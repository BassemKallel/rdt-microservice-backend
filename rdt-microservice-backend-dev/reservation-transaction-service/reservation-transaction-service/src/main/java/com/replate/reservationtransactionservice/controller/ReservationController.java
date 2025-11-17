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

    // create reservation
    @PostMapping("/create")
    public ResponseEntity<ReservationResponse> createReservation(
            @RequestBody ReservationRequest request) {

        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.ok(response);
    }

    // Confirm reservation
    @PostMapping("/confirm")
    public ResponseEntity<ReservationResponse> confirmReservation(
            @RequestBody ConfirmReservationRequest request) {

        ReservationResponse response = reservationService.confirmReservation(request);
        return ResponseEntity.ok(response);
    }
}