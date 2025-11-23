package com.replate.reservationtransactionservice.controller;

import com.replate.reservationtransactionservice.dto.ConfirmReservationRequest;
import com.replate.reservationtransactionservice.dto.ReservationRequest;
import com.replate.reservationtransactionservice.dto.ReservationResponse;
import com.replate.reservationtransactionservice.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    @GetMapping("/my-history")
    public ResponseEntity<List<ReservationResponse>> getMyHistory(Authentication authentication) {
        // 1. Récupérer l'ID de l'utilisateur connecté (depuis le Token)
        Long userId = (Long) authentication.getPrincipal();

        // 2. Récupérer son rôle
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("INDIVIDUAL");

        // 3. Appeler le service avec ces infos
        List<ReservationResponse> history = reservationService.getReservationsByUser(userId, role);

        return ResponseEntity.ok(history);
    }


}