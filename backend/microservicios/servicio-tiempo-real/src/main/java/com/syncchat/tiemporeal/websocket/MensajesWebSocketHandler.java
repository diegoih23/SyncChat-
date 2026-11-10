package com.syncchat.tiemporeal.websocket;

import com.syncchat.tiemporeal.sesiones.SesionesConectadas;
import com.syncchat.tiemporeal.seguridad.JwtTokenValidator;
import java.net.URI;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class MensajesWebSocketHandler extends TextWebSocketHandler {
    static final String USUARIO_ID = "usuarioId";
    private final SesionesConectadas sesiones;
    private final JwtTokenValidator jwt;
    public MensajesWebSocketHandler(SesionesConectadas sesiones, JwtTokenValidator jwt) {
        this.sesiones = sesiones; this.jwt = jwt;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String usuarioId = jwt.usuarioId(token(session.getUri()));
        if (usuarioId == null || usuarioId.isBlank()) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        session.getAttributes().put(USUARIO_ID, usuarioId);
        sesiones.agregar(usuarioId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object usuarioId = session.getAttributes().get(USUARIO_ID);
        if (usuarioId != null) sesiones.quitar(usuarioId.toString(), session);
    }

    private String token(URI uri) {
        if (uri == null) return null;
        return UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
    }
}
