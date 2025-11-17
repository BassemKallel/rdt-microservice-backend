package com.replate.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Autorise les requêtes de votre front-end Angular
        config.setAllowedOrigins(List.of("http://localhost:4200"));

        // Autorise toutes les méthodes (GET, POST, PUT, DELETE, OPTIONS)
        config.addAllowedMethod("*");

        // Autorise tous les en-têtes (y compris 'Authorization' pour le JWT)
        config.addAllowedHeader("*");

        // Autorise l'envoi de credentials (si nécessaire, bien que vous utilisiez des tokens)
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Applique cette configuration à toutes les routes (/**)
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}