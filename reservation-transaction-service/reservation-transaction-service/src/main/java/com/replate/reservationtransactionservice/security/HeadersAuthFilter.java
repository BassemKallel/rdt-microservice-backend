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

    // 🔒 DOIT ÊTRE LE MÊME QUE DANS LA GATEWAY
    private static final String EXPECTED_SECRET = "Replate_Super_Secret_Key_2025";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // --- 🟢 DÉBUT DE LA VÉRIFICATION DU SECRET ---
        String receivedSecret = request.getHeader("X-Internal-Secret");
        String uri = request.getRequestURI();

        // On autorise explicitement /webhook (Stripe) et /actuator (Monitoring) sans secret
        boolean isPublicEndpoint = uri.startsWith("/actuator") || uri.startsWith("/webhook");

        if (!isPublicEndpoint) {
            // Pour tout le reste (ex: /reservations), le secret est OBLIGATOIRE
            if (receivedSecret == null || !receivedSecret.equals(EXPECTED_SECRET)) {
                log.warn("⛔ Accès rejeté : Secret invalide ou manquant pour {}", uri);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Internal Secret Missing");
                return; // On arrête la requête ici
            }
        }
        // --- 🟢 FIN DE LA VÉRIFICATION ---

        // Le reste de la logique d'authentification utilisateur continue ici...
        final String userId = request.getHeader("X-User-Id");
        final String userRole = request.getHeader("X-User-Role");

        if (userId != null && userRole != null) {
            String role = userRole.trim();
            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }

            List<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority(role)
            );

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    Long.valueOf(userId.trim()),
                    null,
                    authorities
            );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}