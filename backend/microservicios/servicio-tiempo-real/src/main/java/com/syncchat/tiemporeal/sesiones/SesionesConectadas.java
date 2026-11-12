package com.syncchat.tiemporeal.sesiones;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class SesionesConectadas {
    private final ConcurrentHashMap<String, Set<WebSocketSession>> sesiones = new ConcurrentHashMap<>();

    public void agregar(String usuarioId, WebSocketSession session) {
        sesiones.computeIfAbsent(usuarioId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }
    public void quitar(String usuarioId, WebSocketSession session) {
        Set<WebSocketSession> delUsuario = sesiones.get(usuarioId);
        if (delUsuario == null) return;
        delUsuario.remove(session);
        if (delUsuario.isEmpty()) sesiones.remove(usuarioId, delUsuario);
    }
    public Set<WebSocketSession> delUsuario(String usuarioId) {
        return sesiones.getOrDefault(usuarioId, Set.of());
    }
    public int total() { return sesiones.values().stream().mapToInt(Set::size).sum(); }
}
