package com.syncchat.mensajes;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Mensaje(UUID id, UUID conversacionId, UUID remitenteId, String contenido, OffsetDateTime creadoEn) {}
