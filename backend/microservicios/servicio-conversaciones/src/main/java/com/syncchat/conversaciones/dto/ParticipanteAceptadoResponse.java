package com.syncchat.conversaciones.dto;

import java.util.UUID;

/** Identidad interna para consumo servicio-a-servicio; nunca se expone al frontend. */
public record ParticipanteAceptadoResponse(UUID usuarioId, String nombre) {}
