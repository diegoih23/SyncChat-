package com.syncchat.mensajes;

import java.util.UUID;

public record CrearMensaje(UUID conversacionId, UUID remitenteId, String contenido) {}
