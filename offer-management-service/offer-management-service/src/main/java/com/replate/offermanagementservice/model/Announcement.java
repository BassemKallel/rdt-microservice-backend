package com.replate.offermanagementservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long merchantId; // ID du propriétaire, lié au JWT

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private AnnouncementType announcementType;

    @Enumerated(EnumType.STRING)
    private ModerationStatus moderationStatus = ModerationStatus.PENDING_REVIEW;


    private Double price;

    private String imageUrl1;

    private LocalDateTime expiryDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}