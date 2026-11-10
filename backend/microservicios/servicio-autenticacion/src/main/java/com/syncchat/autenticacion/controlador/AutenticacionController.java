package com.syncchat.autenticacion.controlador;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import com.syncchat.autenticacion.dto.*;
import com.syncchat.autenticacion.servicio.AutenticacionService;

@RestController
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
