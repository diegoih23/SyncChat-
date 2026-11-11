package com.syncchat.conversaciones.servicio;

import com.syncchat.conversaciones.dto.ConversacionRequest;
import com.syncchat.conversaciones.dto.ConversacionResponse;
import com.syncchat.conversaciones.entidad.Conversacion;
import com.syncchat.conversaciones.repositorio.ConversacionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConversacionService {
    private final ConversacionRepository repositorio;

    public ConversacionService(ConversacionRepository repositorio) {
        this.repositorio = repositorio;
    }

    public ConversacionResponse crear(ConversacionRequest request, UUID usuarioId) {
        Conversacion nueva = new Conversacion(UUID.randomUUID(), request.nombre().trim(), usuarioId, Instant.now());
        return ConversacionResponse.desde(repositorio.save(nueva));
    }

    public List<ConversacionResponse> listar(UUID usuarioId) {
        return repositorio.findByCreadoPorOrderByCreadoEnDesc(usuarioId).stream()
                .map(ConversacionResponse::desde)
                .toList();
    }

    public ConversacionResponse obtener(UUID id) {
        return ConversacionResponse.desde(buscar(id));
    }

    public void eliminar(UUID id, UUID usuarioId) {
        Conversacion c = buscar(id);
        if (!c.getCreadoPor().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo quien creó la conversación puede eliminarla");
        }
        repositorio.delete(c);
    }

    private Conversacion buscar(UUID id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La conversación no existe"));
    }
}
