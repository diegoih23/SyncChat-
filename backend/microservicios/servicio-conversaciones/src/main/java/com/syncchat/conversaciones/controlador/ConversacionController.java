package com.syncchat.conversaciones.controlador;

import com.syncchat.conversaciones.dto.ConversacionRequest;
import com.syncchat.conversaciones.dto.ConversacionResponse;
import com.syncchat.conversaciones.dto.InvitacionRequest;
import com.syncchat.conversaciones.dto.InvitacionResponse;
import com.syncchat.conversaciones.dto.ParticipanteResponse;
import com.syncchat.conversaciones.dto.ParticipanteAceptadoResponse;
import com.syncchat.conversaciones.servicio.ConversacionService;
import com.syncchat.conversaciones.servicio.TokenService;
import com.syncchat.conversaciones.servicio.TokenServicioInterno;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/conversaciones")
public class ConversacionController {
    private final ConversacionService servicio;
    private final TokenService tokens;
    private final TokenServicioInterno tokenInterno;

    public ConversacionController(ConversacionService servicio, TokenService tokens, TokenServicioInterno tokenInterno) {
        this.servicio = servicio;
        this.tokens = tokens;
        this.tokenInterno = tokenInterno;
    }

    @PostMapping
    public ResponseEntity<ConversacionResponse> crear(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
                                                      @Valid @RequestBody ConversacionRequest request) {
        UUID usuario = tokens.usuarioDesdeHeader(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(request, usuario));
    }

    @GetMapping
    public List<ConversacionResponse> listar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return servicio.listar(tokens.usuarioDesdeHeader(auth));
    }

    @GetMapping("/invitaciones/pendientes")
    public List<InvitacionResponse> invitacionesPendientes(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return servicio.invitacionesPendientes(tokens.usuarioDesdeHeader(auth));
    }

    @PostMapping("/{id}/invitaciones")
    public ResponseEntity<Void> invitar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID id,
            @Valid @RequestBody InvitacionRequest request) {
        UUID actor = tokens.usuarioDesdeHeader(auth);
        servicio.invitarPorCorreo(id, actor, request.correo(), auth);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/invitaciones/{id}/aceptar")
    public ResponseEntity<Void> aceptarInvitacion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID id) {
        servicio.responderInvitacion(id, tokens.usuarioDesdeHeader(auth), true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invitaciones/{id}/rechazar")
    public ResponseEntity<Void> rechazarInvitacion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID id) {
        servicio.responderInvitacion(id, tokens.usuarioDesdeHeader(auth), false);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/invitaciones")
    public ResponseEntity<Void> cancelarInvitacion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID id,
            @Valid @RequestBody InvitacionRequest request) {
        UUID actor = tokens.usuarioDesdeHeader(auth);
        servicio.cancelarInvitacionPorCorreo(id, actor, request.correo(), auth);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/participantes/{usuarioId}")
    public ResponseEntity<Void> retirarParticipante(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID id,
            @PathVariable UUID usuarioId) {
        servicio.retirarParticipante(id, tokens.usuarioDesdeHeader(auth), usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/salir")
    public ResponseEntity<Void> salir(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID id) {
        servicio.salir(id, tokens.usuarioDesdeHeader(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/participantes")
    public List<ParticipanteResponse> participantes(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID id) {
        UUID usuario = tokens.usuarioDesdeHeader(auth);
        return servicio.listarParticipantes(id, usuario, auth);
    }

    @GetMapping("/{id}")
    public ConversacionResponse obtener(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
                                        @PathVariable UUID id) {
        return servicio.obtener(id, tokens.usuarioDesdeHeader(auth));
    }

    @GetMapping("/{id}/participantes/aceptados")
    public List<UUID> destinatariosAceptados(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @RequestHeader(value = "X-SyncChat-Service-Token", required = false) String serviceToken,
            @PathVariable UUID id) {
        tokenInterno.validar(serviceToken);
        return servicio.destinatariosAceptados(id, tokens.usuarioDesdeHeader(auth));
    }

    @GetMapping("/{id}/participantes/aceptados/detalles")
    public List<ParticipanteAceptadoResponse> participantesAceptadosConNombre(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @RequestHeader(value = "X-SyncChat-Service-Token", required = false) String serviceToken,
            @PathVariable UUID id) {
        tokenInterno.validar(serviceToken);
        return servicio.participantesAceptadosConNombre(id, tokens.usuarioDesdeHeader(auth), auth);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
                                         @PathVariable UUID id) {
        servicio.eliminar(id, tokens.usuarioDesdeHeader(auth));
        return ResponseEntity.noContent().build();
    }
}
