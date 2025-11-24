package com.replate.offermanagementservice.repository;

import com.replate.offermanagementservice.model.Announcement;
import com.replate.offermanagementservice.model.AnnouncementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findAllByMerchantId(Long merchantId);
    List<Announcement> findByAnnouncementType(AnnouncementType announcementType);
}