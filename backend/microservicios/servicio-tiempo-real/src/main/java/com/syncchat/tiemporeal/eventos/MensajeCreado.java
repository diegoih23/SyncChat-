package com.syncchat.tiemporeal.eventos;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

public record MensajeCreado(String id, String conversacionId, String remitenteId, String remitenteNombre,
        List<String> destinatarios, String contenido, Instant creadoEn) {
    public static MensajeCreado desde(JsonNode json) {
        List<String> destinatarios = json.has("destinatarios") && json.get("destinatarios").isArray()
                ? java.util.stream.StreamSupport.stream(json.get("destinatarios").spliterator(), false)
                        .map(JsonNode::asText).toList() : List.of();
        return new MensajeCreado(texto(json, "id"), texto(json, "conversacionId"),
                texto(json, "remitenteId"), texto(json, "remitenteNombre"), destinatarios, texto(json, "contenido"),
                instante(json));
    }
    private static Instant instante(JsonNode json) {
        if (!json.hasNonNull("creadoEn")) return Instant.now();
        JsonNode fecha = json.get("creadoEn");
        if (fecha.isNumber()) {
            BigDecimal segundos = fecha.decimalValue();
            long epochSeconds = segundos.longValue();
            int nanos = segundos.subtract(BigDecimal.valueOf(epochSeconds))
                    .movePointRight(9).intValue();
            return Instant.ofEpochSecond(epochSeconds, nanos);
        }
        return Instant.parse(fecha.asText());
    }
    private static String texto(JsonNode json, String campo) { return json.hasNonNull(campo) ? json.get(campo).asText() : null; }
}
