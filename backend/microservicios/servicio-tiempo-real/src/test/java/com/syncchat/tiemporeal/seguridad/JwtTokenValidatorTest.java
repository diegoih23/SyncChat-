package com.syncchat.tiemporeal.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtTokenValidatorTest {
    private static final String SECRET = "syncchat-test-secret-key-with-at-least-32-bytes";

    @Test
    void obtieneElUsuarioDesdeElSubject() {
        String id = UUID.randomUUID().toString();
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder().subject(id).issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(key).compact();
        assertEquals(id, new JwtTokenValidator(SECRET).usuarioId(token));
    }

    @Test
    void rechazaTokenInvalido() {
        assertNull(new JwtTokenValidator(SECRET).usuarioId("token-invalido"));
    }
}
