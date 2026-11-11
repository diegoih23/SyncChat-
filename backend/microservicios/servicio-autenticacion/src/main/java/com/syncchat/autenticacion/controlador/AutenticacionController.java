package com.syncchat.autenticacion.controlador;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.syncchat.autenticacion.dto.*;
import com.syncchat.autenticacion.servicio.AutenticacionService;

@RestController
@Validated
@RequestMapping("/")
public class AutenticacionController {
    private final AutenticacionService servicio;
    public AutenticacionController(AutenticacionService servicio) { this.servicio = servicio; }
    @PostMapping({"/registro", "/register"})
    public Respuestas.Autenticacion registrar(@Valid @RequestBody RegistroRequest request) { return servicio.registrar(request); }
    @PostMapping("/login")
    public Respuestas.Autenticacion iniciarSesion(@Valid @RequestBody LoginRequest request) { return servicio.iniciarSesion(request); }
    @GetMapping("/perfil/{id}")
    public Respuestas.UsuarioResponse perfil(@PathVariable UUID id) { return servicio.perfil(id); }
    @GetMapping("/usuarios/buscar")
    public Respuestas.ContactoResponse buscarContacto(
            @RequestHeader(value = "X-SyncChat-Service-Token", required = false) String serviceToken,
            @RequestParam @Email @NotBlank String correo) {
        return servicio.buscarContacto(correo, serviceToken);
    }
    @GetMapping("/usuarios/{id}/interno")
    public Respuestas.ContactoResponse buscarContactoPorId(
            @RequestHeader(value = "X-SyncChat-Service-Token", required = false) String serviceToken,
            @PathVariable UUID id) {
        return servicio.buscarContactoPorId(id, serviceToken);
    }
    @GetMapping("/contactos/buscar")
    public List<Respuestas.ContactoPublicoResponse> buscarContactos(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam @NotBlank String consulta) {
        return servicio.buscarContactos(consulta, authorization);
    }
    @PostMapping("/recuperar")
    public Respuestas.Recuperacion recuperar(@Valid @RequestBody RecuperarRequest request) { return servicio.recuperar(request.correo()); }
    @PostMapping("/restablecer")
    public Respuestas.Mensaje restablecer(@Valid @RequestBody RestablecerRequest request) {
        servicio.restablecer(request); return new Respuestas.Mensaje("Contraseña restablecida correctamente");
    }
    @GetMapping("/check-email")
    public Respuestas.EmailStatus comprobarCorreo(@RequestParam @Email @NotBlank String correo) {
        return new Respuestas.EmailStatus(servicio.correoRegistrado(correo));
    }
}
