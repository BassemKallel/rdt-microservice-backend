package com.replate.reservationtransactionservice.client;

import com.replate.offermanagementservice.dto.AnnouncementRequest;
import com.replate.offermanagementservice.model.Announcement;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "offer-management-service", url = "http://localhost:8084")
public interface AnnouncementClient {

    @GetMapping("/announcements/{id}")
    Announcement getAnnouncementById(@PathVariable Long id);
}
