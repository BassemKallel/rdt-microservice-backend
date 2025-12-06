package com.replate.offermanagementservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    private static final String INTERNAL_SECRET = "Replate_Super_Secret_Key_2025";

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // 1. Toujours injecter le secret pour s'authentifier entre services
            template.header("X-Internal-Secret", INTERNAL_SECRET);

            // 2. Propager le token utilisateur s'il existe (Optionnel mais utile)
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    template.header("Authorization", authHeader);
                }
            }
        };
    }
}