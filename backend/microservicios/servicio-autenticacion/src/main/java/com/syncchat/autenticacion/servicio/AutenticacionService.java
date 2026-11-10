package com.syncchat.autenticacion.servicio;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.syncchat.autenticacion.controlador.AutenticacionController;
import com.syncchat.autenticacion.dto.*;
import com.syncchat.autenticacion.entidad.Usuario;
import com.syncchat.autenticacion.repositorio.UsuarioRepository;

@Service
public class AutenticacionService {
    private final UsuarioRepository usuarios;
    private final BCryptPasswordEncoder encoder;
    private final Key jwtKey;
    private final long expirationMs;
    private final long recoveryExpirationMs;

    public AutenticacionService(UsuarioRepository usuarios,
            @Value("${syncchat.jwt.secret}") String secret,
            @Value("${syncchat.jwt.expiration-ms}") long expirationMs,
            @Value("${syncchat.recovery.expiration-ms:900000}") long recoveryExpirationMs) {
        this.usuarios = usuarios;
        this.encoder = new BCryptPasswordEncoder();
        this.jwtKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.recoveryExpirationMs = recoveryExpirationMs;
    }

    @Transactional
    public Respuestas.Autenticacion registrar(RegistroRequest request) {
        String correo = normalizar(request.correo());
        if (usuarios.findByCorreo(correo).isPresent()) throw error(HttpStatus.CONFLICT, "El correo ya está registrado");
        Usuario usuario = new Usuario(UUID.randomUUID(), request.nombre().trim(), correo,
                encoder.encode(request.password()), Instant.now());
        usuarios.save(usuario);
        return respuesta(usuario);
    }

    public Respuestas.Autenticacion iniciarSesion(LoginRequest request) {
        Usuario usuario = usuarios.findByCorreo(normalizar(request.correo())).orElse(null);
        if (usuario == null || !encoder.matches(request.password(), usuario.getPasswordHash()))
            throw error(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        return respuesta(usuario);
    }

    public Respuestas.UsuarioResponse perfil(UUID id) {
        return usuarios.findById(id).map(this::usuarioResponse)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @Transactional
    public Respuestas.Recuperacion recuperar(String correo) {
        Usuario usuario = usuarios.findByCorreo(normalizar(correo)).orElse(null);
        // No se revela si el correo existe. El token se devuelve para integrar luego un proveedor de correo.
        if (usuario == null) return new Respuestas.Recuperacion("Si el correo existe, recibirá instrucciones", null);
        String token = UUID.randomUUID().toString();
        usuario.asignarTokenRecuperacion(encoder.encode(token), Instant.now().plusMillis(recoveryExpirationMs));
        usuarios.save(usuario);
        return new Respuestas.Recuperacion("Si el correo existe, recibirá instrucciones", token);
    }

    @Transactional
    public void restablecer(RestablecerRequest request) {
        Usuario usuario = usuarios.findByCorreo(normalizar(request.correo())).orElseThrow(
                () -> error(HttpStatus.BAD_REQUEST, "Token de recuperación inválido o expirado"));
        if (usuario.getResetTokenExpira() == null || usuario.getResetTokenExpira().isBefore(Instant.now())
                || !encoder.matches(request.token(), usuario.getResetTokenHash()))
            throw error(HttpStatus.BAD_REQUEST, "Token de recuperación inválido o expirado");
        usuario.cambiarPassword(encoder.encode(request.password()));
        usuario.limpiarTokenRecuperacion();
        usuarios.save(usuario);
    }

    public boolean correoRegistrado(String correo) { return usuarios.findByCorreo(normalizar(correo)).isPresent(); }

    private Respuestas.Autenticacion respuesta(Usuario usuario) {
        Instant ahora = Instant.now();
        String token = Jwts.builder().subject(usuario.getId().toString()).claim("correo", usuario.getCorreo())
                .claim("nombre", usuario.getNombre()).issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusMillis(expirationMs))).signWith(jwtKey).compact();
        return new Respuestas.Autenticacion(token, usuarioResponse(usuario));
    }
    private Respuestas.UsuarioResponse usuarioResponse(Usuario u) {
        return new Respuestas.UsuarioResponse(u.getId().toString(), u.getNombre(), u.getCorreo());
    }
    private String normalizar(String correo) { return correo.trim().toLowerCase(); }
    private ResponseStatusException error(HttpStatus status, String mensaje) { return new ResponseStatusException(status, mensaje); }
}
