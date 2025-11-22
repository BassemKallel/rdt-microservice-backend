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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HeadersAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HeadersAuthFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String userId = request.getHeader("X-User-Id");
        final String userRole = request.getHeader("X-User-Role");
        String userStatus = request.getHeader("X-User-Status");


        log.debug("--- [OMS HeadersAuthFilter] Headers Reçus ---");
        log.debug("X-User-Id: {}", userId);
        log.debug("X-User-Status: {}", userStatus);

        if (userId != null && userRole != null) {
            String cleanedUserRole = userRole.trim();
            if (!cleanedUserRole.startsWith("ROLE_")) {
                cleanedUserRole = "ROLE_" + cleanedUserRole;
            }

            List<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority(cleanedUserRole)
            );

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    Long.valueOf(userId.trim()),
                    null,
                    authorities
            );

            Map<String, String> details = new HashMap<>();
            details.put("status", userStatus != null ? userStatus : "PENDING");
            authToken.setDetails(details);

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        filterChain.doFilter(request, response);
    }
}