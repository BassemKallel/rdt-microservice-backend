package com.replate.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class GatewayJwtUtil {

    @Value("${jwt.secret}")
    private String SECRET;

    private Key getSignKey() {
        byte[] keyBytes = SECRET.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Valide et décode le token, retourne les claims.
     * @param token Le JWT
     * @return Les claims décodés
     * @throws JwtException Si le token est invalide, expiré ou corrompu.
     */
    public Claims validateAndExtractClaims(String token) throws JwtException {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}