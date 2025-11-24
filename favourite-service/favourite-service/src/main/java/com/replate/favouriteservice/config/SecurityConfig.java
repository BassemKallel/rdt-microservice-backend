package com.replate.favouriteservice.config;

import com.replate.favouriteservice.security.HeadersAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
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
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Seuls les rôles INDIVIDUAL et ASSOCIATION peuvent gérer les favoris
                        .requestMatchers("/favorites/**").hasAnyRole("INDIVIDUAL", "ASSOCIATION")
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(headersAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}