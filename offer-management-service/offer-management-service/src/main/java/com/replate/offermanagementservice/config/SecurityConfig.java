package com.replate.offermanagementservice.config;

// Pas d'import de JwtAuthFilter
import com.replate.offermanagementservice.security.HeadersAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// Pas d'import de UsernamePasswordAuthenticationFilter

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
                        // Routes publiques
                        .requestMatchers("/api/v1/offers/browse").permitAll()
                        .requestMatchers("/api/v1/offers/search").permitAll()
                        .requestMatchers("/api/v1/offers/{id}").permitAll()

                        // Routes MERCHANT (s'appuie sur le rôle injecté par le Gateway)
                        .requestMatchers("/api/v1/offers/create").hasAuthority("ROLE_MERCHANT")
                        .requestMatchers("/api/v1/offers/update/**").hasAuthority("ROLE_MERCHANT")
                        .requestMatchers("/api/v1/offers/delete/**").hasAuthority("ROLE_MERCHANT")
                        .requestMatchers("/api/v1/offers/my-offers").hasAuthority("ROLE_MERCHANT")

                        // Health checks
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/info").permitAll()

                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(headersAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}