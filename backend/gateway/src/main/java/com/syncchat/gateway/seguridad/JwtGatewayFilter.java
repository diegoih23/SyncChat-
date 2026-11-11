package com.syncchat.gateway.seguridad;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {
    private final SecretKey key;
    public JwtGatewayFilter(@Value("${syncchat.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name()) || !requiereToken(path)) return chain.filter(exchange);
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ") || !valido(header.substring(7).trim())) return rechazar(exchange);
        return chain.filter(exchange);
    }
    private boolean requiereToken(String path) {
        return path.startsWith("/api/conversaciones/") || path.equals("/api/conversaciones")
                || path.startsWith("/api/mensajes/") || path.equals("/api/mensajes")
                || path.equals("/api/auth/usuarios/buscar")
                || path.startsWith("/api/auth/usuarios/")
                || path.equals("/api/auth/contactos/buscar");
    }
    private boolean valido(String token) {
        try { Jwts.parser().verifyWith(key).build().parseSignedClaims(token); return true; }
        catch (RuntimeException ex) { return false; }
    }
    private Mono<Void> rechazar(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"status\":401,\"mensaje\":\"Token JWT requerido o inválido\"}".getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
    @Override public int getOrder() { return -100; }
}
