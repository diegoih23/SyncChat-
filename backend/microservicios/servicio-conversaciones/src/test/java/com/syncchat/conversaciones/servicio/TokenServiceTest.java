package com.syncchat.conversaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class TokenServiceTest {
    private static final String SECRET = "syncchat-test-secret-key-with-at-least-32-bytes";

    private SecretKey clave;
    private TokenService tokens;

    @BeforeEach
    void preparar() {
        clave = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        tokens = new TokenService(SECRET);
    }

    @Test
    void extraeElUsuarioDeUnTokenValido() {
        UUID usuario = UUID.randomUUID();
        String token = Jwts.builder().subject(usuario.toString()).signWith(clave).compact();

        assertThat(tokens.usuarioDesdeHeader("Bearer " + token)).isEqualTo(usuario);
    }

    @Test
    void rechazaUnHeaderAusente() {
        assertThatThrownBy(() -> tokens.usuarioDesdeHeader(null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED))
                .hasMessageContaining("Token JWT requerido");
    }

    @Test
    void rechazaUnEsquemaDeAutorizacionInvalido() {
        assertThatThrownBy(() -> tokens.usuarioDesdeHeader("Basic credenciales"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED))
                .hasMessageContaining("Token JWT requerido");
    }

    @Test
    void rechazaUnTokenConFirmaInvalida() {
        UUID usuario = UUID.randomUUID();
        SecretKey otraClave = Keys.hmacShaKeyFor(
                "otra-clave-de-prueba-con-mas-de-32-bytes".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder().subject(usuario.toString()).signWith(otraClave).compact();

        assertThatThrownBy(() -> tokens.usuarioDesdeHeader("Bearer " + token))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED))
                .hasMessageContaining("Token JWT inválido");
    }

    @Test
    void rechazaUnTokenCuyoSubjectNoEsUnUuid() {
        String token = Jwts.builder().subject("no-es-un-uuid").signWith(clave).compact();

        assertThatThrownBy(() -> tokens.usuarioDesdeHeader("Bearer " + token))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED))
                .hasMessageContaining("Token JWT inválido");
    }
}
