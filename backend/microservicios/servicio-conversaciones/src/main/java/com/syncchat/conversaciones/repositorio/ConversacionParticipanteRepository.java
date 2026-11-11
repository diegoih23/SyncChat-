package com.syncchat.conversaciones.repositorio;

import com.syncchat.conversaciones.entidad.ConversacionParticipante;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.syncchat.conversaciones.entidad.EstadoParticipante;

public interface ConversacionParticipanteRepository extends JpaRepository<ConversacionParticipante, UUID> {
    List<ConversacionParticipante> findByConversacionId(UUID conversacionId);
    List<ConversacionParticipante> findByUsuarioId(UUID usuarioId);
    List<ConversacionParticipante> findByUsuarioIdAndEstado(UUID usuarioId, EstadoParticipante estado);
    List<ConversacionParticipante> findByConversacionIdAndEstado(UUID conversacionId, EstadoParticipante estado);
    Optional<ConversacionParticipante> findByConversacionIdAndUsuarioId(UUID conversacionId, UUID usuarioId);
}
