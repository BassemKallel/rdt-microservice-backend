package com.replate.gateway.security;

import com.replate.gateway.security.GatewayJwtUtil;
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

    // Liste des chemins publics qui ne nécessitent pas de JWT
    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/v1/users/register",
            "/api/v1/users/login",
            "/api/v1/offers/browse",
            "/api/v1/offers/search"
    );

    public AuthGatewayFilter(GatewayJwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Contourner les endpoints publics
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

            // 4. Mutate la requête en ajoutant les claims comme en-têtes
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", claims.get("userId", Long.class).toString())
                    .header("X-User-Role", claims.get("role", String.class))
                    .header("X-Is-Validated", claims.get("validated", Boolean.class).toString())
                    .build();

            log.debug("✅ JWT validé. Headers injectés pour UserID: {}", claims.get("userId"));

            // 5. Continuer la chaîne avec la nouvelle requête
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException e) {
            log.warn("❌ JWT Invalide pour la requête {} : {}", path, e.getMessage());
            return onError(exchange, "Invalid or expired authentication token", HttpStatus.UNAUTHORIZED);
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        // Vous pouvez aussi ajouter un body d'erreur ici si nécessaire
        return response.setComplete();
    }

    // Le filtre doit s'exécuter avant le filtre de routage par défaut.
    @Override
    public int getOrder() {
        return -100; // Très haute priorité
    }
}