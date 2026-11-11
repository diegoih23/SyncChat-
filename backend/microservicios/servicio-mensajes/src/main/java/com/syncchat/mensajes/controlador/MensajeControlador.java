package com.syncchat.mensajes.controlador;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.syncchat.mensajes.eventos.MensajeCreado;
import com.syncchat.mensajes.eventos.MensajeEventPublisher;
import com.syncchat.mensajes.modelo.MensajeServicio;
import com.syncchat.mensajes.vista.CrearMensajeVista;
import com.syncchat.mensajes.vista.MensajeVista;
import com.syncchat.mensajes.seguridad.TokenService;
import com.syncchat.mensajes.seguridad.ConversacionesPermisosClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Controlador MVC: gestiona las peticiones HTTP del microservicio.
 * No contiene lógica de negocio ni acceso a datos: recibe vistas,
 * delega en el servicio del modelo y responde con vistas.
 */
@RestController
@RequestMapping("/api/mensajes")
public class MensajeControlador {
    private final MensajeServicio servicio;
    private final MensajeEventPublisher publisher;
    private final TokenService tokens;
    private final ConversacionesPermisosClient permisos;

    public MensajeControlador(MensajeServicio servicio, MensajeEventPublisher publisher,
                              TokenService tokens, ConversacionesPermisosClient permisos) {
        this.servicio = servicio;
        this.publisher = publisher;
        this.tokens = tokens;
        this.permisos = permisos;
    }

    @PostMapping
    public ResponseEntity<MensajeVista> crear(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody CrearMensajeVista nuevo) {
        UUID remitenteId = tokens.usuarioDesdeHeader(authorization);
        List<ConversacionesPermisosClient.ParticipanteAceptado> participantes =
                permisos.participantesAceptados(nuevo.conversacionId(), authorization);
        List<UUID> destinatarios = participantes.stream()
                .map(ConversacionesPermisosClient.ParticipanteAceptado::usuarioId).toList();
        String remitenteNombre = participantes.stream()
                .filter(participante -> participante.usuarioId().equals(remitenteId))
                .map(ConversacionesPermisosClient.ParticipanteAceptado::nombre).findFirst().orElse(null);
        MensajeVista mensaje = MensajeVista.desdeModelo(servicio.crear(
                new com.syncchat.mensajes.modelo.CrearMensaje(nuevo.conversacionId(), remitenteId,
                        nuevo.contenido(), nuevo.idempotencyKey())), remitenteNombre);
        publisher.publicar(new MensajeCreado(mensaje.id(), mensaje.conversacionId(), mensaje.remitenteId(), mensaje.remitenteNombre(),
                destinatarios, mensaje.contenido(), mensaje.creadoEn().toInstant()));
        return ResponseEntity.created(URI.create("/api/mensajes/" + mensaje.id())).body(mensaje);
    }

    @GetMapping("/conversacion/{id}")
    public List<MensajeVista> historial(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID id) {
        tokens.usuarioDesdeHeader(authorization);
        List<ConversacionesPermisosClient.ParticipanteAceptado> participantes = permisos.participantesAceptados(id, authorization);
        var nombres = participantes.stream().collect(java.util.stream.Collectors.toMap(
                ConversacionesPermisosClient.ParticipanteAceptado::usuarioId,
                ConversacionesPermisosClient.ParticipanteAceptado::nombre));
        return servicio.historial(id).stream()
                .map(mensaje -> MensajeVista.desdeModelo(mensaje, nombres.get(mensaje.remitenteId()))).toList();
    }
}
