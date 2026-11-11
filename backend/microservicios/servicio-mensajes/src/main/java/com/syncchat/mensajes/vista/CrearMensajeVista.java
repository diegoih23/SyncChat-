package com.syncchat.mensajes.vista;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Vista de la petición para crear un mensaje: es el JSON que envía el cliente.
 * Se convierte a un comando del modelo antes de ejecutar la lógica de negocio.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CrearMensajeVista(
        @NotNull(message = "Indica la conversación") UUID conversacionId,
        @NotNull(message = "Indica la clave idempotente del mensaje") UUID idempotencyKey,
        @NotBlank(message = "El contenido no puede estar vacío") String contenido) {}
