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

    // RETIREZ "/api/v1/offers/" de cette liste si vous l'avez ajouté
    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/v1/users/register",
            "/api/v1/users/login",
            "/api/v1/offers/browse",
            "/api/v1/offers/search",
            "/api/v1/offers/public",
            "/api/v1/files/upload",
            "/api/v1/offers/browse",
            "/actuator/health"
    );

    public AuthGatewayFilter(GatewayJwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // --- CORRECTION ICI ---
        // 1. Vérification spécifique pour laisser passer GET /offers/{id} (où id est un nombre)
        // Cela empêche de laisser passer /offers/create par erreur
        boolean isPublicOfferDetail = method.equals("GET") && path.matches("^/api/v1/offers/\\d+$");

        // 2. Si c'est dans la liste blanche OU si c'est un détail d'offre public
        if (isPublicOfferDetail || OPEN_ENDPOINTS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // 3. Sinon, on procède à l'authentification standard
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Authorization header missing or invalid", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.validateAndExtractClaims(token);

            String userId = String.valueOf(claims.get("userId"));
            String role = String.valueOf(claims.get("role"));

            // Gestion robuste du status
            Object statusObj = claims.get("status");
            String status;
            if (statusObj != null) {
                status = String.valueOf(statusObj);
            } else {
                Object validatedObj = claims.get("validated");
                status = (validatedObj != null && Boolean.parseBoolean(String.valueOf(validatedObj))) ? "ACTIVE" : "PENDING";
            }

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .header("X-User-Status", status)
                    .build();

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
        return -100;
    }
}