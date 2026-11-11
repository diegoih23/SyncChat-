package com.syncchat.mensajes.vista;

import java.util.UUID;
import com.syncchat.mensajes.modelo.CrearMensaje;

/**
 * Vista de la petición para crear un mensaje: es el JSON que envía el cliente.
 * Se convierte a un comando del modelo antes de ejecutar la lógica de negocio.
 */
public record CrearMensajeVista(UUID conversacionId, UUID remitenteId, String contenido) {

    /** Convierte la petición del cliente en el comando que entiende el modelo. */
    public CrearMensaje aModelo() {
        return new CrearMensaje(conversacionId, remitenteId, contenido);
    }
}
