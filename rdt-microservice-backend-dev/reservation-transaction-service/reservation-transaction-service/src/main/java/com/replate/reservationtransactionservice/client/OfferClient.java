package com.replate.reservationtransactionservice.client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.replate.reservationtransactionservice.dto.ReservationResponse;
import org.springframework.cloud.openfeign.FeignClient;


//Feign client for communicating with the Offer Management Service.
@FeignClient(name = "offer-management-service", url = "${oms.service.url}")
public interface OfferClient {
    @GetMapping("/offers/{id}")
    ReservationResponse getAnnouncementById(@PathVariable("id") Long annonceId);
}

