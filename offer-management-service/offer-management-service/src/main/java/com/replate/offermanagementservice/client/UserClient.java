package com.replate.offermanagementservice.client;

import com.replate.offermanagementservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// "user-management-service" est le nom du service dans Eureka
@FeignClient(name = "user-management-service", url = "${ums.service.url}") // Configurez l'URL dans application.properties si besoin ou laissez Eureka gérer
public interface UserClient {

    @GetMapping("/users/{id}") // Assurez-vous que cet endpoint existe dans UMS
    UserDTO getUserById(@PathVariable("id") Long id);
}