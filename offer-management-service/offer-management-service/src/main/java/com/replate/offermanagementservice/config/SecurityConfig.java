package com.replate.offermanagementservice.config;

import com.replate.offermanagementservice.security.HeadersAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                        .requestMatchers(HttpMethod.GET, "/offers/browse").permitAll()
                        .requestMatchers(HttpMethod.GET, "/offers/{id}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/offers/create").hasRole("MERCHANT")

                        .requestMatchers(HttpMethod.GET, "/offers/my-offers").hasAnyRole("MERCHANT", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/offers/update/**").hasRole("MERCHANT")

                        .requestMatchers(HttpMethod.DELETE, "/offers/delete/**").hasAnyRole("ADMIN", "MERCHANT")

                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(headersAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}