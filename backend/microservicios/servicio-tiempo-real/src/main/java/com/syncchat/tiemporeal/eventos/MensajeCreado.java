package com.syncchat.tiemporeal.eventos;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record MensajeCreado(String id, String conversacionId, String remitenteId,
        String destinatarioId, String contenido, Instant creadoEn) {
    public static MensajeCreado desde(JsonNode json) {
        return new MensajeCreado(texto(json, "id"), texto(json, "conversacionId"),
                texto(json, "remitenteId"), texto(json, "destinatarioId"), texto(json, "contenido"),
                json.hasNonNull("creadoEn") ? Instant.parse(json.get("creadoEn").asText()) : Instant.now());
    }
    private static String texto(JsonNode json, String campo) { return json.hasNonNull(campo) ? json.get(campo).asText() : null; }
}
