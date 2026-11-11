package com.syncchat.mensajes.seguridad;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ConversacionesPermisosClient {
    private final RestClient cliente;
    private final String serviceToken;

    public ConversacionesPermisosClient(RestClient.Builder builder,
            @Value("${syncchat.conversaciones.participantes-url}") String url,
            @Value("${syncchat.conversaciones.service-token}") String serviceToken) {
        this.cliente = builder.baseUrl(url).build();
        this.serviceToken = serviceToken;
    }

    public List<UUID> destinatariosAceptados(UUID conversacionId, String authorization) {
        try {
            List<?> respuesta = cliente.get()
                    .uri("/{id}/participantes/aceptados", conversacionId)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header("X-SyncChat-Service-Token", serviceToken)
                    .retrieve().body(List.class);
            if (respuesta == null) return List.of();
            return respuesta.stream().map(valor -> UUID.fromString(valor.toString())).toList();
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(ex.getStatusCode(),
                    ex.getStatusCode().is4xxClientError() ? "No tienes acceso a esta conversación" : "No fue posible validar permisos de conversación");
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de conversaciones no está disponible para validar permisos");
        }
    }

    public List<ParticipanteAceptado> participantesAceptados(UUID conversacionId, String authorization) {
        try {
            List<ParticipanteAceptado> respuesta = cliente.get()
                    .uri("/{id}/participantes/aceptados/detalles", conversacionId)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header("X-SyncChat-Service-Token", serviceToken)
                    .retrieve().body(new ParameterizedTypeReference<>() {});
            return respuesta == null ? List.of() : respuesta;
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(ex.getStatusCode(),
                    ex.getStatusCode().is4xxClientError() ? "No tienes acceso a esta conversación" : "No fue posible validar permisos de conversación");
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de conversaciones no está disponible para validar permisos");
        }
    }

    public record ParticipanteAceptado(UUID usuarioId, String nombre) {}
}
