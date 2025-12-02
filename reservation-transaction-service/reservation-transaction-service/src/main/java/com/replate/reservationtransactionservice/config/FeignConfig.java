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
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                String userId = request.getHeader("X-User-Id");
                String userRole = request.getHeader("X-User-Role");
                String userStatus = request.getHeader("X-User-Status");

                // 🟢 MODIFICATION : Récupérer le secret entrant
                String internalSecret = request.getHeader("X-Internal-Secret");

                if (userId != null) template.header("X-User-Id", userId);
                if (userRole != null) template.header("X-User-Role", userRole);
                if (userStatus != null) template.header("X-User-Status", userStatus);

                // 🟢 MODIFICATION : Le propager vers le service suivant (OMS)
                if (internalSecret != null) {
                    template.header("X-Internal-Secret", internalSecret);
                }
            }
        };
    }
}