package com.syncchat.conversaciones.dto;

import com.syncchat.conversaciones.entidad.Conversacion;
import java.time.Instant;

public record ConversacionResponse(String id, String nombre, String creadoPor, Instant creadoEn) {
    public static ConversacionResponse desde(Conversacion c) {
        return new ConversacionResponse(c.getId().toString(), c.getNombre(),
                c.getCreadoPor().toString(), c.getCreadoEn());
    }
}
