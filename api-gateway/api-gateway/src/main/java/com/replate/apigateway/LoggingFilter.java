package com.replate.apigateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Component
public class LoggingFilter implements GlobalFilter {

    final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Avant d'envoyer la requête
        logger.info("🌍 REQUÊTE REÇUE: {}", exchange.getRequest().getPath());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // Après que les filtres ont tourné (juste avant l'envoi ou après la réponse)
            var route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            var url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);

            if (url != null) {
                logger.info("➡️ ROUTÉE VERS: " + url);
            }
        }));
    }
}