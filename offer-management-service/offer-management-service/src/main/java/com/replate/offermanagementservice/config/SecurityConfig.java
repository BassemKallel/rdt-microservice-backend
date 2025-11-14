package com.replate.offermanagementservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Le constructeur n'a plus besoin d'injecter JwtAuthFilter

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 🔓 Routes publiques
                        .requestMatchers("/api/v1/offers/browse").permitAll()
                        .requestMatchers("/api/v1/offers/search").permitAll()
                        .requestMatchers("/api/v1/offers/{id}").permitAll()

                        // 🔐 Routes MERCHANT (s'appuie sur le rôle injecté par le Gateway)
                        .requestMatchers("/api/v1/offers/create").hasRole("MERCHANT")
                        .requestMatchers("/api/v1/offers/update/**").hasRole("MERCHANT")
                        .requestMatchers("/api/v1/offers/delete/**").hasRole("MERCHANT")
                        .requestMatchers("/api/v1/offers/my-offers").hasRole("MERCHANT")

                        // Health checks
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/info").permitAll()

                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        // Le filtre JWT personnalisé est maintenant dans le Gateway

        return http.build();
    }
}