package com.syncchat.tiemporeal.seguridad;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenValidator {
    private final SecretKey key;
    public JwtTokenValidator(@Value("${syncchat.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public String usuarioId(String token) {
        if (token == null || token.isBlank()) return null;
        try { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject(); }
        catch (RuntimeException ex) { return null; }
    }
}
