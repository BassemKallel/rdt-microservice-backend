package com.replate.offermanagementservice.security;

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

        final String userId = request.getHeader("X-User-Id");
        final String userRole = request.getHeader("X-User-Role"); // Lecture du header
        final String isValidated = request.getHeader("X-Is-Validated");

        log.debug("--- [OMS HeadersAuthFilter] Headers Reçus ---");
        log.debug("Path: {}", request.getRequestURI());
        log.debug("X-User-Id Header: '{}'", userId);
        log.debug("X-User-Role Header: '{}'", userRole);
        log.debug("X-Is-Validated Header: '{}'", isValidated);
        log.debug("-------------------------------------------------");

        // Nous forçons l'authentification si les headers sont présents
        if (userId != null && userRole != null) {

            String cleanedUserRole = userRole.trim();

            List<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + cleanedUserRole)
            );

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    Long.valueOf(userId.trim()), // Ajout de .trim() par sécurité
                    null,
                    authorities
            );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.debug("Contexte de sécurité Spring FORCÉ pour ROLE: {}", cleanedUserRole);
        }

        filterChain.doFilter(request, response);
    }
}