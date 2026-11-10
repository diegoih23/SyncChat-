package com.syncchat.autenticacion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import com.syncchat.autenticacion.dto.LoginRequest;
import com.syncchat.autenticacion.dto.RegistroRequest;
import com.syncchat.autenticacion.entidad.Usuario;
import com.syncchat.autenticacion.repositorio.UsuarioRepository;
import com.syncchat.autenticacion.servicio.AutenticacionService;

@ExtendWith(MockitoExtension.class)
class AutenticacionServiceTest {
    @Mock UsuarioRepository usuarios;
    private AutenticacionService servicio;

    @BeforeEach
    void preparar() {
        servicio = new AutenticacionService(usuarios,
                "syncchat-test-secret-key-with-at-least-32-bytes", 86_400_000, 900_000);
    }

    @Test
    void registraContrasenaCifradaYDevuelveJwt() {
        when(usuarios.findByCorreo("ana@example.com")).thenReturn(Optional.empty());
        when(usuarios.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var respuesta = servicio.registrar(new RegistroRequest(
                "Ana", "ANA@example.com", "secreto123"));

        assertNotNull(respuesta.token());
        verify(usuarios).save(any(Usuario.class));
    }

    @Test
    void rechazaCorreoDuplicado() {
        Usuario existente = new Usuario(UUID.randomUUID(), "Ana", "ana@example.com", "hash", java.time.Instant.now());
        when(usuarios.findByCorreo("ana@example.com")).thenReturn(Optional.of(existente));

        assertThrows(ResponseStatusException.class, () -> servicio.registrar(
                new RegistroRequest("Ana", "ana@example.com", "secreto123")));
        verify(usuarios, never()).save(any());
    }

    @Test
    void rechazaCredencialesIncorrectas() {
        when(usuarios.findByCorreo("ana@example.com")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> servicio.iniciarSesion(
                new LoginRequest("ana@example.com", "incorrecta")));
    }
}
