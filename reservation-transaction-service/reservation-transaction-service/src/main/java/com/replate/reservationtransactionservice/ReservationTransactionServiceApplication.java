package com.replate.reservationtransactionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.replate.reservationtransactionservice")
@EnableFeignClients(basePackages = "com.replate.reservationtransactionservice.client") // only Feign interfaces
@EnableDiscoveryClient
@EnableScheduling
public class ReservationTransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservationTransactionServiceApplication.class, args);
    }
}

