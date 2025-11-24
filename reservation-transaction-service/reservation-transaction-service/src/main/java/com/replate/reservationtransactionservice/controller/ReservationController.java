package com.replate.reservationtransactionservice.controller;

import com.replate.reservationtransactionservice.dto.ConfirmReservationRequest;
import com.replate.reservationtransactionservice.dto.ReservationRequest;
import com.replate.reservationtransactionservice.dto.ReservationResponse;
import com.replate.reservationtransactionservice.exception.UnauthorizedActionException;
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

    // --- CRÉATION (Client) ---
    @PostMapping("/create")
    public ResponseEntity<ReservationResponse> createReservation(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.createReservation(request));
    }

    // --- CONFIRMATION (Marchand) ---
    @PostMapping("/confirm")
    public ResponseEntity<ReservationResponse> confirmReservation(@RequestBody ConfirmReservationRequest request) {
        return ResponseEntity.ok(reservationService.confirmReservation(request));
    }

    // --- REJET (Marchand) ---
    @PostMapping("/reject")
    public ResponseEntity<ReservationResponse> rejectReservation(@RequestBody ConfirmReservationRequest request) {
        return ResponseEntity.ok(reservationService.rejectReservation(request));
    }

    // --- DÉTAIL (Client - Polling) ---
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    // --- HISTORIQUE (Client & Marchand) ---
    // S'adapte automatiquement au rôle de l'utilisateur
    @GetMapping("/my-history")
    public ResponseEntity<List<ReservationResponse>> getMyHistory(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("INDIVIDUAL");

        return ResponseEntity.ok(reservationService.getReservationsByUser(userId, role));
    }

    // --- ADMIN (Vue Globale) ---
    @GetMapping("/admin/history")
    public ResponseEntity<List<ReservationResponse>> getAllHistory(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.contains("ADMIN"));

        if (!isAdmin) {
            throw new UnauthorizedActionException("Accès réservé aux administrateurs.");
        }

        return ResponseEntity.ok(reservationService.getAllReservations());
    }
}