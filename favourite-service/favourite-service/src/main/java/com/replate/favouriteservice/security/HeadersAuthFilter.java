package com.replate.favouriteservice.security;


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

    // 🔒 DOIT ÊTRE LE MÊME QUE DANS LA GATEWAY
    private static final String EXPECTED_SECRET = "Replate_Super_Secret_Key_2025";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // --- 🟢 DÉBUT DE LA VÉRIFICATION DU SECRET ---
        String receivedSecret = request.getHeader("X-Internal-Secret");

        // On ignore la vérification pour les endpoints techniques comme Actuator si nécessaire
        if (!request.getRequestURI().startsWith("/actuator")) {
            if (receivedSecret == null || !receivedSecret.equals(EXPECTED_SECRET)) {
                log.warn("⛔ Accès rejeté : Secret invalide ou manquant pour {}", request.getRequestURI());
                // On renvoie directement une erreur 403 Forbidden et on coupe la chaîne
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Internal Secret Missing");
                return;
            }
        }
        // --- 🟢 FIN DE LA VÉRIFICATION ---

        // Le reste de la logique existante continue ici...
        final String userId = request.getHeader("X-User-Id");
        final String userRole = request.getHeader("X-User-Role");
        String userStatus = request.getHeader("X-User-Status");

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

            // Note: Adaptez cette partie "details" selon ce qui existait déjà dans le service spécifique
            // (ex: RTS n'avait pas "status" dans details, mais OMS si).
            Map<String, String> details = new HashMap<>();
            details.put("status", userStatus != null ? userStatus : "PENDING");
            authToken.setDetails(details);

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}