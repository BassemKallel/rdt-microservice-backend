package com.replate.offermanagementservice.service;

import com.replate.offermanagementservice.dto.AnnouncementRequest;
import com.replate.offermanagementservice.exception.AccountNotValidatedException;
import com.replate.offermanagementservice.exception.ForbiddenPermissionException;
import com.replate.offermanagementservice.exception.ResourceNotFoundException;
import com.replate.offermanagementservice.model.Announcement;
import com.replate.offermanagementservice.model.AnnouncementType;
import com.replate.offermanagementservice.repository.AnnouncementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @InjectMocks
    private AnnouncementService announcementService;

    @Test
    void createAnnouncement_Success() {
        AnnouncementRequest request = new AnnouncementRequest();
        request.setTitle("Vente pain");
        request.setAnnouncementType(AnnouncementType.SALE);
        request.setPrice(5.0);

        when(announcementRepository.save(any(Announcement.class))).thenAnswer(i -> {
            Announcement a = i.getArgument(0);
            a.setId(1L);
            return a;
        });

        Announcement result = announcementService.createAnnouncement(request, 100L, true); // isValidated = true

        assertNotNull(result);
        assertEquals(100L, result.getMerchantId());
        verify(announcementRepository).save(any(Announcement.class));
    }

    @Test
    void createAnnouncement_NotValidated_ThrowsException() {
        AnnouncementRequest request = new AnnouncementRequest();
        // isValidated = false
        assertThrows(AccountNotValidatedException.class,
                () -> announcementService.createAnnouncement(request, 100L, false));

        verify(announcementRepository, never()).save(any());
    }

    @Test
    void updateAnnouncement_Success() {
        Announcement existing = new Announcement();
        existing.setId(1L);
        existing.setMerchantId(100L);
        existing.setTitle("Old Title");

        AnnouncementRequest request = new AnnouncementRequest();
        request.setTitle("New Title");

        when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(i -> i.getArgument(0));

        Announcement updated = announcementService.updateAnnouncement(1L, request, 100L); // Same user

        assertEquals("New Title", updated.getTitle());
    }

    @Test
    void updateAnnouncement_Forbidden() {
        Announcement existing = new Announcement();
        existing.setId(1L);
        existing.setMerchantId(100L); // Owner ID 100

        AnnouncementRequest request = new AnnouncementRequest();

        when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));

        // User ID 200 tries to update
        assertThrows(ForbiddenPermissionException.class,
                () -> announcementService.updateAnnouncement(1L, request, 200L));
    }

    @Test
    void getById_NotFound() {
        when(announcementRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> announcementService.getById(99L));
    }
}