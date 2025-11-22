package com.replate.reservationtransactionservice.config;

import com.replate.reservationtransactionservice.security.HeadersAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration("reservationSecurityConfig")
@EnableWebSecurity
public class SecurityConfig {

    private final HeadersAuthFilter headersAuthFilter;

    public SecurityConfig(HeadersAuthFilter headersAuthFilter) {
        this.headersAuthFilter = headersAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Les endpoints publics (si besoin)
                        .requestMatchers("/actuator/**").permitAll()
                        // Les endpoints sécurisés
                        .requestMatchers("/reservations/**", "/payments/**").authenticated()
                        .requestMatchers("/webhook/**").permitAll()

                        // Vos autres routes protégées
                        .requestMatchers("/reservations/**", "/payments/**").authenticated()
                        .anyRequest().permitAll()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Ajout du HeadersAuthFilter avant le filtre standard
                .addFilterBefore(headersAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Note : Le PasswordEncoder n'est plus nécessaire ici si on ne fait pas d'authentification locale par mot de passe.
}