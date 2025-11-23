package com.replate.reservationtransactionservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    private static final Logger log = LoggerFactory.getLogger(FeignConfig.class);

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // Récupérer la requête HTTP entrante (celle venant de la Gateway)
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // Liste des headers à propager
                String userId = request.getHeader("X-User-Id");
                String userRole = request.getHeader("X-User-Role");
                String userStatus = request.getHeader("X-User-Status");
                String authHeader = request.getHeader("Authorization");

                // Injection dans la requête sortante (vers OMS)
                if (userId != null) {
                    template.header("X-User-Id", userId);
                    log.debug("Propagation Header X-User-Id: {}", userId);
                }
                if (userRole != null) template.header("X-User-Role", userRole);
                if (userStatus != null) template.header("X-User-Status", userStatus);

                // On propage aussi le Bearer token original par sécurité
                if (authHeader != null) {
                    template.header("Authorization", authHeader);
                }
            }
        };
    }
}