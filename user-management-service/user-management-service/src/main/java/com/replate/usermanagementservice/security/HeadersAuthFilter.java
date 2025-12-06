package com.replate.usermanagementservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class HeadersAuthFilter extends OncePerRequestFilter {

    private static final String EXPECTED_SECRET = "Replate_Super_Secret_Key_2025";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String receivedSecret = request.getHeader("X-Internal-Secret");

        // 🟢 MODIFICATION : Si un Token JWT est présent, on laisse le JwtAuthFilter gérer (Priorité à l'Admin)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Sinon (pas de token), on vérifie le secret pour les appels internes (ex: OMS -> UMS)
        if (EXPECTED_SECRET.equals(receivedSecret)) {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "INTERNAL_SERVICE",
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_INTERNAL"))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}