package com.replate.usermanagementservice.config;

import com.replate.usermanagementservice.security.HeadersAuthFilter;
import com.replate.usermanagementservice.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final HeadersAuthFilter headersAuthFilter; // Pour le Secret Interne
    private final UserDetailsService userDetailsService;

    // Injection des dépendances via le constructeur
    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          HeadersAuthFilter headersAuthFilter,
                          UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.headersAuthFilter = headersAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 1. Accès Public (Inscription / Connexion)
                        .requestMatchers("/users/register/**", "/users/login/**").permitAll()

                        // 2. Monitoring (Actuator)
                        .requestMatchers("/actuator/**").permitAll()

                        // 3. Accès Interne (Pour que OMS puisse récupérer le nom du marchand)
                        // Nécessite le rôle INTERNAL (donné par HeadersAuthFilter si le secret est bon)
                        .requestMatchers("/users/{id}").hasAnyRole("ADMIN", "INTERNAL", "MERCHANT", "INDIVIDUAL", "ASSOCIATION")

                        // 4. Accès Admin
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 5. Tout le reste nécessite d'être authentifié
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())

                // Ordre des filtres : Secret Interne d'abord, puis JWT
                .addFilterBefore(headersAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}