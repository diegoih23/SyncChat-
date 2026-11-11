package com.syncchat.mensajes.vista;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.syncchat.mensajes.modelo.Mensaje;

/**
 * Vista del mensaje: es la representación JSON que se devuelve al cliente.
 * El modelo nunca se expone directamente; el controlador lo transforma en esta vista.
 */
public record MensajeVista(UUID id, UUID conversacionId, UUID remitenteId, String contenido, OffsetDateTime creadoEn) {

    /** Convierte un mensaje del modelo en su vista para el cliente. */
    public static MensajeVista desdeModelo(Mensaje mensaje) {
        return new MensajeVista(mensaje.id(), mensaje.conversacionId(), mensaje.remitenteId(),
                mensaje.contenido(), mensaje.creadoEn());
    }
}
