package com.syncchat.conversaciones.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversacion_participantes", uniqueConstraints =
        @UniqueConstraint(name = "uq_conversacion_participante", columnNames = {"conversacion_id", "usuario_id"}))
public class ConversacionParticipante {
    @Id
    private UUID id;

    @Column(name = "conversacion_id", nullable = false)
    private UUID conversacionId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoParticipante estado;

    @Column(nullable = false)
    private boolean administrador;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected ConversacionParticipante() {}

    public ConversacionParticipante(UUID id, UUID conversacionId, UUID usuarioId,
                                    EstadoParticipante estado, boolean administrador, Instant ahora) {
        this.id = id;
        this.conversacionId = conversacionId;
        this.usuarioId = usuarioId;
        this.estado = estado;
        this.administrador = administrador;
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
    }

    public UUID getId() { return id; }
    public UUID getConversacionId() { return conversacionId; }
    public UUID getUsuarioId() { return usuarioId; }
    public EstadoParticipante getEstado() { return estado; }
    public boolean isAdministrador() { return administrador; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }

    public void cambiarEstado(EstadoParticipante nuevoEstado, Instant ahora) {
        boolean permitido = switch (estado) {
            case PENDING -> nuevoEstado == EstadoParticipante.ACCEPTED
                    || nuevoEstado == EstadoParticipante.DECLINED || nuevoEstado == EstadoParticipante.REMOVED;
            case ACCEPTED -> nuevoEstado == EstadoParticipante.LEFT || nuevoEstado == EstadoParticipante.REMOVED;
            case DECLINED, LEFT, REMOVED -> nuevoEstado == EstadoParticipante.PENDING;
        };
        if (!permitido) throw new IllegalStateException("Transición de participante no permitida: " + estado + " -> " + nuevoEstado);
        estado = nuevoEstado;
        actualizadoEn = ahora;
        // Las invitaciones reabiertas nunca conservan privilegios administrativos anteriores.
        if (nuevoEstado == EstadoParticipante.PENDING) administrador = false;
    }
}
