package com.replate.reservationtransactionservice.client;

import com.replate.reservationtransactionservice.dto.AnnouncementResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Add contextId to avoid bean name conflicts
@FeignClient(name = "offer-management-service", url = "${oms.service.url}", contextId = "announcementClient")
public interface AnnouncementClient {

    @GetMapping("/offers/{id}")
    AnnouncementResponse getAnnouncementById(@PathVariable("id") Long id);
}