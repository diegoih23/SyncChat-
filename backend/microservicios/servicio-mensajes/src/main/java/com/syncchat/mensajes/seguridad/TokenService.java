package com.syncchat.mensajes.seguridad;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TokenService {
    private final SecretKey key;

    public TokenService(@Value("${syncchat.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public UUID usuarioDesdeHeader(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT requerido");
        }
        try {
            String subject = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(authorization.substring(7).trim()).getPayload().getSubject();
            return UUID.fromString(subject);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT inválido o expirado");
        }
    }
}
