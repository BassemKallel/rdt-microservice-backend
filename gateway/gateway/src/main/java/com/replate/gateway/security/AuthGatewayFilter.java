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

    // 🔒 LE SECRET PARTAGÉ (Idéalement à mettre dans application.properties)
    private static final String INTERNAL_SECRET = "Replate_Super_Secret_Key_2025";

    private final GatewayJwtUtil jwtUtil;

    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/v1/users/register",
            "/api/v1/users/login",
            "/api/v1/offers/browse",
            "/api/v1/offers/search",
            "/api/v1/offers/public",
            "/api/v1/files/upload",
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
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        boolean isPublicOfferDetail = method.equals("GET") && path.matches("^/api/v1/offers/\\d+$");
        boolean isPublicEndpoint = isPublicOfferDetail || OPEN_ENDPOINTS.stream().anyMatch(path::startsWith);

        // CAS 1 : Accès Public SANS Token (Visiteur Anonyme)
        if (isPublicEndpoint && (authHeader == null || !authHeader.startsWith("Bearer "))) {
            // 🟢 CORRECTION : On doit injecter le secret MÊME pour les anonymes
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Internal-Secret", INTERNAL_SECRET) // <--- L'AJOUT CRUCIAL
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        // CAS 2 : Accès Privé SANS Token (Erreur)
        if (!isPublicEndpoint && (authHeader == null || !authHeader.startsWith("Bearer "))) {
            return onError(exchange, "Authorization header missing or invalid", HttpStatus.UNAUTHORIZED);
        }

        // CAS 3 : Accès AVEC Token (Utilisateur Connecté - Public ou Privé)
        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.validateAndExtractClaims(token);
            String userId = String.valueOf(claims.get("userId"));
            String role = String.valueOf(claims.get("role"));

            Object statusObj = claims.get("status");
            String status = (statusObj != null) ? String.valueOf(statusObj) : "PENDING";

            // Injection des infos utilisateurs ET du secret
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .header("X-User-Status", status)
                    .header("X-Internal-Secret", INTERNAL_SECRET) // Le secret est aussi ici
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