package com.replate.reservationtransactionservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class HeadersAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HeadersAuthFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Récupération des headers injectés par la Gateway
        final String userId = request.getHeader("X-User-Id");
        final String userRole = request.getHeader("X-User-Role");

        if (userId != null && userRole != null) {
            log.debug("Authentification via Headers - UserID: {}, Role: {}", userId, userRole);

            String role = userRole.trim();
            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }

            List<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority(role)
            );

            // Création de l'objet d'authentification
            // Principal = userId (Long) pour faciliter l'utilisation dans les contrôleurs
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    Long.valueOf(userId.trim()), // Principal
                    null,                        // Credentials (null car pré-authentifié)
                    authorities                  // Autorités
            );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Injection dans le contexte de sécurité
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } else {
            log.debug("Headers d'authentification manquants (X-User-Id ou X-User-Role).");
        }

        filterChain.doFilter(request, response);
    }
}