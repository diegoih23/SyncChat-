package com.syncchat.conversaciones.controlador;

import com.syncchat.conversaciones.dto.ConversacionRequest;
import com.syncchat.conversaciones.dto.ConversacionResponse;
import com.syncchat.conversaciones.servicio.ConversacionService;
import com.syncchat.conversaciones.servicio.TokenService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversaciones")
public class ConversacionController {
    private final ConversacionService servicio;
    private final TokenService tokens;

    public ConversacionController(ConversacionService servicio, TokenService tokens) {
        this.servicio = servicio;
        this.tokens = tokens;
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

    @GetMapping("/{id}")
    public ConversacionResponse obtener(@PathVariable UUID id) {
        return servicio.obtener(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
                                         @PathVariable UUID id) {
        servicio.eliminar(id, tokens.usuarioDesdeHeader(auth));
        return ResponseEntity.noContent().build();
    }
}
