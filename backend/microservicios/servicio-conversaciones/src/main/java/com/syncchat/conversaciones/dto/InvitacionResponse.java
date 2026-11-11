package com.syncchat.conversaciones.dto;

import java.time.Instant;

public record InvitacionResponse(String conversacionId, String nombre, Instant invitadaEn) {}
