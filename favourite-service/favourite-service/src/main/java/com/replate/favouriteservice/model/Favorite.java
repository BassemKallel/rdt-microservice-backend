package com.replate.favouriteservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


@Entity
@Table(name = "favorites", uniqueConstraints = {
        // Empêche un utilisateur de mettre plusieurs fois le même item en favori
        @UniqueConstraint(columnNames = {"userId", "targetId", "targetType"})
})
@Data
@NoArgsConstructor
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // L'utilisateur qui a mis en favori
    @Column(nullable = false)
    private Long userId;

    // L'ID de l'entité mise en favori (Annonce ou Marchand)
    @Column(nullable = false)
    private Long targetId;

    // Le type de l'entité
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}