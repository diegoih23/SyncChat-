package com.syncchat.mensajes;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/mensajes")
public class MensajeControlador {
    private final MensajeRepositorio repo;

    public MensajeControlador(MensajeRepositorio repo) { this.repo = repo; }

    @PostMapping
    public ResponseEntity<Mensaje> crear(@RequestBody CrearMensaje nuevo) {
        // Rechaza mensajes incompletos antes de guardarlos.
        if (nuevo.conversacionId() == null || nuevo.remitenteId() == null || nuevo.contenido() == null || nuevo.contenido().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversacionId, remitenteId y contenido son obligatorios");
        }
        Mensaje mensaje = repo.guardar(nuevo);
        return ResponseEntity.created(URI.create("/api/mensajes/" + mensaje.id())).body(mensaje);
    }

    @GetMapping("/conversacion/{id}")
    public List<Mensaje> historial(@PathVariable UUID id) { return repo.buscarPorConversacion(id); }
}
