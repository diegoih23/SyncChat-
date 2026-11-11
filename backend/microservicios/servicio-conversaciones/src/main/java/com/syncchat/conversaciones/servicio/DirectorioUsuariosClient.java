package com.syncchat.conversaciones.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.UUID;

@Component
public class DirectorioUsuariosClient {
    private final RestClient cliente;
    private final RestClient usuarios;
    private final String serviceToken;

    public DirectorioUsuariosClient(RestClient.Builder builder,
                                   @Value("${syncchat.auth.lookup-url:http://localhost:8080/api/auth/usuarios/buscar}") String url,
                                   @Value("${syncchat.auth.user-url:http://localhost:8080/api/auth/usuarios}") String userUrl,
                                   @Value("${syncchat.auth.service-token:syncchat-directory-development-token}") String serviceToken) {
        this.cliente = builder.baseUrl(url).build();
        this.usuarios = builder.baseUrl(userUrl).build();
        this.serviceToken = serviceToken;
    }

    public Contacto buscarPorId(UUID usuarioId, String authorization) {
        try {
            Contacto contacto = usuarios.get()
                    .uri("/{id}/interno", usuarioId)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header("X-SyncChat-Service-Token", serviceToken)
                    .retrieve().body(Contacto.class);
            if (contacto == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Directorio de usuarios no disponible");
            return contacto;
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No fue posible validar acceso al directorio");
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró una cuenta participante");
        } catch (HttpClientErrorException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No fue posible consultar el nombre del participante");
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Directorio de usuarios no disponible");
        }
    }

    public Contacto buscarPorCorreo(String correo, String authorization) {
        try {
            Contacto contacto = cliente.get()
                    .uri(uri -> uri.queryParam("correo", correo).build())
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header("X-SyncChat-Service-Token", serviceToken)
                    .retrieve()
                    .body(Contacto.class);
            if (contacto == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Directorio de usuarios no disponible");
            return contacto;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró una cuenta con ese correo");
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT inválido o expirado");
        } catch (HttpClientErrorException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No fue posible validar la cuenta invitada");
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Directorio de usuarios no disponible");
        }
    }

    public record Contacto(String id, String nombre) {}
}
