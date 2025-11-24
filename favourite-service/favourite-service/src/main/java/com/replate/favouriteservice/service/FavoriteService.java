package com.replate.favouriteservice.service;


import com.replate.favouriteservice.exception.ResourceNotFoundException;
import com.replate.favouriteservice.model.Favorite;
import com.replate.favouriteservice.model.TargetType;
import com.replate.favouriteservice.repository.FavoriteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    public FavoriteService(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    public Favorite addFavorite(Long userId, Long targetId, String targetTypeStr) {
        TargetType targetType = TargetType.valueOf(targetTypeStr.toUpperCase());

        if (favoriteRepository.existsByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)) {
            // Lève une exception HTTP 409 (Conflict)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet élément est déjà dans vos favoris.");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setTargetId(targetId);
        favorite.setTargetType(targetType);

        return favoriteRepository.save(favorite);
    }

    public void removeFavorite(Long userId, Long targetId, String targetTypeStr) {
        TargetType targetType = TargetType.valueOf(targetTypeStr.toUpperCase());

        Favorite favorite = favoriteRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)
                .orElseThrow(() -> new ResourceNotFoundException("Favori non trouvé ou n'existe pas."));

        favoriteRepository.delete(favorite);
    }

    public List<Favorite> getMyFavorites(Long userId) {
        return favoriteRepository.findAllByUserId(userId);
    }
}