package com.replate.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthGatewayFilter.class);

    private final GatewayJwtUtil jwtUtil;

    // Liste des chemins publics
    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/v1/users/register",
            "/api/v1/users/login",
            "/api/v1/offers/browse",
            "/api/v1/offers/search",
            "/api/v1/offers/public",
            "/api/v1/files/upload",
            "/api/v1/reservations/create", // Souvent public pour initier (selon votre flux)
            "/actuator/health"
    );

    public AuthGatewayFilter(GatewayJwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Contourner les endpoints publics
        // Vérification plus large (startsWith) pour éviter les bloquages sur les sous-ressources
        if (OPEN_ENDPOINTS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // 2. Vérifier le header Authorization
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Authorization header missing or invalid", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        try {
            // 3. Valider le token et extraire les claims
            Claims claims = jwtUtil.validateAndExtractClaims(token);

            // Extraction sécurisée des claims
            String userId = String.valueOf(claims.get("userId"));
            String role = String.valueOf(claims.get("role"));

            // Récupération du statut : on tente "status" (nouveau) puis "validated" (ancien)
            Object statusObj = claims.get("status");
            String status;

            if (statusObj != null) {
                status = String.valueOf(statusObj);
            } else {
                // Fallback sur l'ancien champ booléen
                Object validatedObj = claims.get("validated");
                if (validatedObj != null && Boolean.parseBoolean(String.valueOf(validatedObj))) {
                    status = "ACTIVE";
                } else {
                    status = "PENDING";
                }
            }

            // 4. Mutate la requête en ajoutant les claims comme en-têtes
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .header("X-User-Status", status) // Le header attendu par les services
                    .build();

            log.debug("✅ JWT validé. Injection Headers -> ID: {}, Role: {}, Status: {}", userId, role, status);

            // 5. Continuer la chaîne avec la nouvelle requête
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException e) {
            log.warn("❌ JWT Invalide: {}", e.getMessage());
            return onError(exchange, "Invalid Token", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("❌ Erreur Gateway: {}", e.getMessage());
            return onError(exchange, "Gateway Error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100; // Priorité haute
    }
}