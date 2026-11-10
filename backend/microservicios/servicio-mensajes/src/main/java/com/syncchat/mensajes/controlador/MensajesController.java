package com.syncchat.mensajes.controlador;

import com.syncchat.mensajes.eventos.MensajeCreado;
import com.syncchat.mensajes.eventos.MensajeEventPublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/mensajes", "/api/mensajes"})
public class MensajesController {
    private final MensajeEventPublisher publisher;
    public MensajesController(MensajeEventPublisher publisher) { this.publisher = publisher; }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MensajeCreado crear(@Valid @RequestBody CrearMensajeRequest request) {
        MensajeCreado evento = new MensajeCreado(UUID.randomUUID(), request.conversacionId(), request.remitenteId(),
                request.destinatarioId(), request.contenido().trim(), Instant.now());
        publisher.publicar(evento);
        return evento;
    }

    public record CrearMensajeRequest(@NotNull(message = "Indica la conversación") UUID conversacionId,
            @NotNull(message = "Indica el remitente") UUID remitenteId,
            @NotNull(message = "Indica el destinatario") UUID destinatarioId,
            @NotBlank(message = "El contenido no puede estar vacío") String contenido) {}
}
