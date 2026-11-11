package com.syncchat.mensajes.modelo;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servicio del modelo: contiene la lógica de negocio del microservicio.
 * Valida las reglas antes de hablar con el repositorio, de forma que el
 * controlador no necesita conocerlas.
 */
@Service
public class MensajeServicio {
    private final MensajeRepositorio repositorio;

    public MensajeServicio(MensajeRepositorio repositorio) { this.repositorio = repositorio; }

    /** Crea un mensaje tras validar que esté completo. */
    public Mensaje crear(CrearMensaje nuevo) {
        // Rechaza mensajes incompletos antes de guardarlos.
        if (nuevo.conversacionId() == null || nuevo.remitenteId() == null || nuevo.contenido() == null || nuevo.contenido().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversacionId, remitenteId y contenido son obligatorios");
        }
        return repositorio.guardar(nuevo);
    }

    /** Devuelve el historial de mensajes de una conversación. */
    public List<Mensaje> historial(UUID conversacionId) {
        return repositorio.buscarPorConversacion(conversacionId);
    }
}
