package com.syncchat.tiemporeal.websocket;

import com.syncchat.tiemporeal.sesiones.SesionesConectadas;
import com.syncchat.tiemporeal.seguridad.JwtTokenValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MensajesWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(MensajesWebSocketHandler.class);
    static final String USUARIO_ID = "usuarioId";
    private final SesionesConectadas sesiones;
    private final JwtTokenValidator jwt;
    private final ObjectMapper mapper;
    private final RestClient conversaciones;
    private final String serviceToken;
    public MensajesWebSocketHandler(SesionesConectadas sesiones, JwtTokenValidator jwt, ObjectMapper mapper,
            RestClient.Builder builder,
            @Value("${syncchat.conversaciones.participantes-url:http://localhost:8082/api/conversaciones}") String url,
            @Value("${syncchat.conversaciones.service-token:syncchat-directory-development-token}") String serviceToken) {
        this.sesiones = sesiones;
        this.jwt = jwt;
        this.mapper = mapper;
        this.conversaciones = builder.baseUrl(url).build();
        this.serviceToken = serviceToken;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String usuarioId = jwt.usuarioId(token(session.getUri()));
        if (usuarioId == null || usuarioId.isBlank()) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        session.getAttributes().put(USUARIO_ID, usuarioId);
        session.getAttributes().put("jwt", token(session.getUri()));
        sesiones.agregar(usuarioId, session);
        log.debug("Sesión WebSocket autenticada; sesiones activas: {}", sesiones.total());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage mensaje) {
        try {
            JsonNode evento = mapper.readTree(mensaje.getPayload());
            if (!"typing".equals(evento.path("tipo").asText())) return;
            UUID conversacionId = UUID.fromString(evento.path("conversacionId").asText());
            String usuarioId = String.valueOf(session.getAttributes().get(USUARIO_ID));
            Object token = session.getAttributes().get("jwt");
            if (token == null) return;

            var autorizados = conversaciones.get()
                    .uri("/{id}/participantes/aceptados/detalles", conversacionId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("X-SyncChat-Service-Token", serviceToken)
                    .retrieve().body(new ParameterizedTypeReference<java.util.List<Participante>>() {});
            if (autorizados == null || autorizados.stream().noneMatch(p -> usuarioId.equals(p.usuarioId()))) return;

            String notificacion = mapper.writeValueAsString(java.util.Map.of(
                    "tipo", "typing", "conversacionId", conversacionId.toString(),
                    "usuarioId", usuarioId, "escribiendo", evento.path("escribiendo").asBoolean()));
            for (Participante participante : autorizados) {
                if (usuarioId.equals(participante.usuarioId())) continue;
                for (WebSocketSession destino : sesiones.delUsuario(participante.usuarioId())) {
                    if (destino.isOpen()) synchronized (destino) { destino.sendMessage(new TextMessage(notificacion)); }
                }
            }
        } catch (Exception exception) {
            log.debug("Se ignoró un evento de escritura no válido o no autorizado");
        }
    }

    private record Participante(String usuarioId, String nombre) {}

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object usuarioId = session.getAttributes().get(USUARIO_ID);
        if (usuarioId != null) {
            sesiones.quitar(usuarioId.toString(), session);
            log.debug("Sesión WebSocket cerrada; sesiones activas: {}", sesiones.total());
        }
    }

    private String token(URI uri) {
        if (uri == null) return null;
        return UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
    }
}
