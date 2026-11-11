package com.syncchat.conversaciones.servicio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TokenServicioInterno {
    private final byte[] token;

    public TokenServicioInterno(@Value("${syncchat.auth.service-token}") String token) {
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    public void validar(String recibido) {
        if (recibido == null || !MessageDigest.isEqual(token, recibido.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credencial interna inválida");
        }
    }
}
