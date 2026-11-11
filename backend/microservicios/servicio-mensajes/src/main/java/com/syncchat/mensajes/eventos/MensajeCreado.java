package com.syncchat.mensajes.eventos;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

public record MensajeCreado(UUID id, UUID conversacionId, UUID remitenteId, String remitenteNombre,
        List<UUID> destinatarios, String contenido, Instant creadoEn) {}
