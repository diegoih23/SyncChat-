package com.syncchat.mensajes.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Modelo del mensaje: representa los datos de negocio y de persistencia.
 * No sabe nada sobre HTTP ni sobre la vista.
 */
public record Mensaje(UUID id, UUID conversacionId, UUID remitenteId, String contenido, OffsetDateTime creadoEn) {}
