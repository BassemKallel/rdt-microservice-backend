package com.replate.favouriteservice.repository;

import com.replate.favouriteservice.model.Favorite;
import com.replate.favouriteservice.model.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findAllByUserId(Long userId);

    boolean existsByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, TargetType targetType);

    Optional<Favorite> findByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, TargetType targetType);
}