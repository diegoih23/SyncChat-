package com.syncchat.mensajes.eventos;

import java.time.Instant;
import java.util.UUID;

public record MensajeCreado(UUID id, UUID conversacionId, UUID remitenteId,
        UUID destinatarioId, String contenido, Instant creadoEn) {}
