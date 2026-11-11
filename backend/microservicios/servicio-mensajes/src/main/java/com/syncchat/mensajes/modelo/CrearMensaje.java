package com.syncchat.mensajes.modelo;

import java.util.UUID;

/**
 * Comando del modelo con los datos necesarios para guardar un mensaje nuevo.
 * La vista lo construye a partir de la petición del cliente.
 */
public record CrearMensaje(UUID conversacionId, UUID remitenteId, String contenido) {}
