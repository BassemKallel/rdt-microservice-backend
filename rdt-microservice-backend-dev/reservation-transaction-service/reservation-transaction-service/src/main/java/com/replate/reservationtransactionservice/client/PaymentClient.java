package com.replate.reservationtransactionservice.client;

import com.replate.reservationtransactionservice.dto.PaymentRequest;
import com.replate.reservationtransactionservice.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

//Feign client for communicating with the external Payment service.
@FeignClient(name = "payment-service", url = "${payment.service.url}")
public interface PaymentClient {

    @PostMapping("/payments/create")
    PaymentResponse createPayment(@RequestBody PaymentRequest paymentRequest);

}
