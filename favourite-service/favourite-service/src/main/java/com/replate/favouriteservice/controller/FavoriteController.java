package com.replate.favouriteservice.controller;

import com.replate.favouriteservice.model.Favorite;
import com.replate.favouriteservice.service.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/favorites") // Préfixe géré par la Gateway (ex: /api/v1/favorites)
public class FavoriteController {


    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    // Récupère l'ID utilisateur injecté par la Gateway
    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    // Ajout d'une annonce en favori
    @PostMapping("/announcements/{id}")
    public ResponseEntity<Favorite> addAnnouncementFavorite(
            @PathVariable("id") Long announcementId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        Favorite favorite = favoriteService.addFavorite(userId, announcementId, "ANNOUNCEMENT");
        return ResponseEntity.ok(favorite);
    }

    // Retrait d'une annonce des favoris
    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<String> removeAnnouncementFavorite(
            @PathVariable("id") Long announcementId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        favoriteService.removeFavorite(userId, announcementId, "ANNOUNCEMENT");
        return ResponseEntity.ok("Annonce retirée des favoris.");
    }

    // Ajout d'un marchand en favori (en utilisant l'ID de l'utilisateur Merchant)
    @PostMapping("/merchants/{id}")
    public ResponseEntity<Favorite> addMerchantFavorite(
            @PathVariable("id") Long merchantId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        Favorite favorite = favoriteService.addFavorite(userId, merchantId, "MERCHANT");
        return ResponseEntity.ok(favorite);
    }

    // Retrait d'un marchand des favoris
    @DeleteMapping("/merchants/{id}")
    public ResponseEntity<String> removeMerchantFavorite(
            @PathVariable("id") Long merchantId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        favoriteService.removeFavorite(userId, merchantId, "MERCHANT");
        return ResponseEntity.ok("Marchand retiré des favoris.");
    }

    // Récupérer la liste des favoris de l'utilisateur
    @GetMapping("/my-list")
    public ResponseEntity<List<Favorite>> getMyFavorites(Authentication authentication) {
        Long userId = getUserId(authentication);
        List<Favorite> favorites = favoriteService.getMyFavorites(userId);
        return ResponseEntity.ok(favorites);
    }
}