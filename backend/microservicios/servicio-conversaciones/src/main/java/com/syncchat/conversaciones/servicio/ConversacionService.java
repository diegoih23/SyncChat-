package com.syncchat.conversaciones.servicio;

import com.syncchat.conversaciones.dto.ConversacionRequest;
import com.syncchat.conversaciones.dto.ConversacionResponse;
import com.syncchat.conversaciones.dto.InvitacionResponse;
import com.syncchat.conversaciones.dto.ParticipanteResponse;
import com.syncchat.conversaciones.dto.ParticipanteAceptadoResponse;
import com.syncchat.conversaciones.entidad.Conversacion;
import com.syncchat.conversaciones.entidad.ConversacionParticipante;
import com.syncchat.conversaciones.entidad.EstadoParticipante;
import com.syncchat.conversaciones.repositorio.ConversacionRepository;
import com.syncchat.conversaciones.repositorio.ConversacionParticipanteRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Comparator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversacionService {
    private final ConversacionRepository repositorio;
    private final ConversacionParticipanteRepository participantes;
    private final DirectorioUsuariosClient directorio;

    public ConversacionService(ConversacionRepository repositorio, ConversacionParticipanteRepository participantes,
                               DirectorioUsuariosClient directorio) {
        this.repositorio = repositorio;
        this.participantes = participantes;
        this.directorio = directorio;
    }

    @Transactional
    public ConversacionResponse crear(ConversacionRequest request, UUID usuarioId) {
        Instant ahora = Instant.now();
        Conversacion nueva = new Conversacion(UUID.randomUUID(), request.nombre().trim(), usuarioId, ahora);
        Conversacion guardada = repositorio.save(nueva);
        participantes.save(new ConversacionParticipante(UUID.randomUUID(), guardada.getId(), usuarioId,
                EstadoParticipante.ACCEPTED, true, ahora));
        return ConversacionResponse.desde(guardada);
    }

    public List<ConversacionResponse> listar(UUID usuarioId) {
        List<UUID> ids = participantes.findByUsuarioIdAndEstado(usuarioId, EstadoParticipante.ACCEPTED).stream()
                .map(ConversacionParticipante::getConversacionId).toList();
        return repositorio.findAllById(ids).stream()
                .sorted(Comparator.comparing(Conversacion::getCreadoEn).reversed())
                .map(ConversacionResponse::desde)
                .toList();
    }

    public ConversacionResponse obtener(UUID id, UUID usuarioId) {
        requireAccepted(id, usuarioId);
        return ConversacionResponse.desde(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<UUID> destinatariosAceptados(UUID conversacionId, UUID usuarioId) {
        requireAccepted(conversacionId, usuarioId);
        return participantes.findByConversacionIdAndEstado(conversacionId, EstadoParticipante.ACCEPTED).stream()
                .map(ConversacionParticipante::getUsuarioId).toList();
    }

    @Transactional(readOnly = true)
    public List<ParticipanteAceptadoResponse> participantesAceptadosConNombre(
            UUID conversacionId, UUID usuarioId, String authorization) {
        requireAccepted(conversacionId, usuarioId);
        return participantes.findByConversacionIdAndEstado(conversacionId, EstadoParticipante.ACCEPTED).stream()
                .map(participante -> {
                    DirectorioUsuariosClient.Contacto contacto = directorio.buscarPorId(
                            participante.getUsuarioId(), authorization);
                    return new ParticipanteAceptadoResponse(participante.getUsuarioId(), contacto.nombre());
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<ParticipanteResponse> listarParticipantes(UUID conversacionId, UUID usuarioId, String authorization) {
        requireAccepted(conversacionId, usuarioId);
        return participantes.findByConversacionId(conversacionId).stream()
                .filter(participante -> participante.getEstado() != EstadoParticipante.REMOVED)
                .map(participante -> {
                    DirectorioUsuariosClient.Contacto contacto = directorio.buscarPorId(
                            participante.getUsuarioId(), authorization);
                    return new ParticipanteResponse(contacto.nombre(), participante.getEstado().name(),
                            participante.isAdministrador());
                }).toList();
    }

    @Transactional
    public void invitar(UUID conversacionId, UUID actorId, UUID invitadoId) {
        buscar(conversacionId);
        requireAdmin(conversacionId, actorId);
        if (invitadoId == null || invitadoId.equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes invitarte a ti mismo");
        }

        var existente = participantes.findByConversacionIdAndUsuarioId(conversacionId, invitadoId);
        if (existente.isEmpty()) {
            participantes.save(new ConversacionParticipante(UUID.randomUUID(), conversacionId, invitadoId,
                    EstadoParticipante.PENDING, false, Instant.now()));
            return;
        }
        ConversacionParticipante participante = existente.get();
        if (participante.getEstado() == EstadoParticipante.PENDING || participante.getEstado() == EstadoParticipante.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya está invitado o participa en la conversación");
        }
        participante.cambiarEstado(EstadoParticipante.PENDING, Instant.now());
        participantes.save(participante);
    }

    @Transactional
    public void invitarPorCorreo(UUID conversacionId, UUID actorId, String correo, String authorization) {
        buscar(conversacionId);
        requireAdmin(conversacionId, actorId);
        DirectorioUsuariosClient.Contacto contacto = directorio.buscarPorCorreo(correo, authorization);
        invitar(conversacionId, actorId, UUID.fromString(contacto.id()));
    }

    @Transactional
    public void cancelarInvitacionPorCorreo(UUID conversacionId, UUID actorId, String correo, String authorization) {
        buscar(conversacionId);
        requireAdmin(conversacionId, actorId);
        DirectorioUsuariosClient.Contacto contacto = directorio.buscarPorCorreo(correo, authorization);
        UUID invitadoId = UUID.fromString(contacto.id());
        ConversacionParticipante invitado = buscarParticipante(conversacionId, invitadoId);
        if (invitado.getEstado() != EstadoParticipante.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede cancelar una invitación pendiente");
        }
        invitado.cambiarEstado(EstadoParticipante.REMOVED, Instant.now());
        participantes.save(invitado);
    }

    @Transactional(readOnly = true)
    public List<InvitacionResponse> invitacionesPendientes(UUID usuarioId) {
        return participantes.findByUsuarioIdAndEstado(usuarioId, EstadoParticipante.PENDING).stream()
                .map(participante -> {
                    Conversacion conversacion = buscar(participante.getConversacionId());
                    return new InvitacionResponse(conversacion.getId().toString(), conversacion.getNombre(),
                            participante.getActualizadoEn());
                }).toList();
    }

    @Transactional
    public void responderInvitacion(UUID conversacionId, UUID usuarioId, boolean aceptar) {
        ConversacionParticipante participante = buscarParticipante(conversacionId, usuarioId);
        if (participante.getEstado() != EstadoParticipante.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay una invitación pendiente para responder");
        }
        participante.cambiarEstado(aceptar ? EstadoParticipante.ACCEPTED : EstadoParticipante.DECLINED, Instant.now());
        participantes.save(participante);
    }

    @Transactional
    public void salir(UUID conversacionId, UUID usuarioId) {
        ConversacionParticipante participante = requireAccepted(conversacionId, usuarioId);
        protegerUltimoAdministrador(conversacionId, participante);
        participante.cambiarEstado(EstadoParticipante.LEFT, Instant.now());
        participantes.save(participante);
    }

    @Transactional
    public void retirarParticipante(UUID conversacionId, UUID actorId, UUID usuarioId) {
        buscar(conversacionId);
        requireAdmin(conversacionId, actorId);
        if (actorId.equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usa la opción de salir para retirarte del grupo");
        }
        ConversacionParticipante objetivo = buscarParticipante(conversacionId, usuarioId);
        if (objetivo.getEstado() != EstadoParticipante.ACCEPTED && objetivo.getEstado() != EstadoParticipante.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario no tiene una invitación activa ni participa en el grupo");
        }
        protegerUltimoAdministrador(conversacionId, objetivo);
        objetivo.cambiarEstado(EstadoParticipante.REMOVED, Instant.now());
        participantes.save(objetivo);
    }

    public void eliminar(UUID id, UUID usuarioId) {
        Conversacion c = buscar(id);
        requireAdmin(id, usuarioId);
        repositorio.delete(c);
    }

    private ConversacionParticipante requireAccepted(UUID conversacionId, UUID usuarioId) {
        ConversacionParticipante participante = buscarParticipante(conversacionId, usuarioId);
        if (participante.getEstado() != EstadoParticipante.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Debes aceptar la invitación para acceder a esta conversación");
        }
        return participante;
    }

    private ConversacionParticipante buscarParticipante(UUID conversacionId, UUID usuarioId) {
        return participantes.findByConversacionIdAndUsuarioId(conversacionId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres participante de esta conversación"));
    }

    private void requireAdmin(UUID conversacionId, UUID usuarioId) {
        ConversacionParticipante participante = requireAccepted(conversacionId, usuarioId);
        if (!participante.isAdministrador()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta acción requiere ser administrador de la conversación");
        }
    }

    private void protegerUltimoAdministrador(UUID conversacionId, ConversacionParticipante objetivo) {
        if (!objetivo.isAdministrador() || objetivo.getEstado() != EstadoParticipante.ACCEPTED) return;
        boolean hayOtroAdministrador = participantes.findByConversacionId(conversacionId).stream()
                .anyMatch(p -> !p.getId().equals(objetivo.getId()) && p.isAdministrador()
                        && p.getEstado() == EstadoParticipante.ACCEPTED);
        if (!hayOtroAdministrador) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede retirar al último administrador activo");
        }
    }

    private Conversacion buscar(UUID id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La conversación no existe"));
    }
}
