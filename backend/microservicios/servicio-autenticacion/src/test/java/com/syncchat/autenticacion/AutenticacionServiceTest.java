package com.syncchat.autenticacion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
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
                "syncchat-test-secret-key-with-at-least-32-bytes", 86_400_000, 900_000,
                "test-directory-service-token");
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

    @Test
    void buscaContactoPorCorreoSoloConCredencialInternaYNoDevuelveElCorreo() {
        when(usuarios.findByCorreo("ana@example.com")).thenReturn(Optional.empty());
        when(usuarios.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var sesion = servicio.registrar(new RegistroRequest("Ana", "ana@example.com", "secreto123"));
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        Usuario ana = captor.getValue();
        when(usuarios.findByCorreo("ana@example.com")).thenReturn(Optional.of(ana));

        var contacto = servicio.buscarContacto("ANA@example.com", "test-directory-service-token");

        assertEquals(ana.getId().toString(), contacto.id());
        assertEquals("Ana", contacto.nombre());
    }

    @Test
    void noPermiteBuscarContactosSinCredencialInterna() {
        assertThrows(ResponseStatusException.class,
                () -> servicio.buscarContacto("ana@example.com", "token-invalido"));
    }

    @Test
    void busquedaInternaPorIdRequiereCredencialDeServicio() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(new Usuario(
                usuarioId, "Ana", "ana@example.com", "hash", java.time.Instant.now())));

        var contacto = servicio.buscarContactoPorId(usuarioId, "test-directory-service-token");

        assertEquals(usuarioId.toString(), contacto.id());
        assertEquals("Ana", contacto.nombre());
        assertThrows(ResponseStatusException.class,
                () -> servicio.buscarContactoPorId(usuarioId, "incorrecto"));
    }

    @Test
    void buscarContactosDevuelveSoloNombreYAvatarYNoIncluyeIdentificadores() {
        when(usuarios.findByCorreo("ana@example.com")).thenReturn(Optional.empty());
        when(usuarios.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var sesion = servicio.registrar(new RegistroRequest("Ana", "ana@example.com", "secreto123"));
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        Usuario contacto = new Usuario(UUID.randomUUID(), "Beatriz", "b@example.com", "hash", java.time.Instant.now());
        when(usuarios.buscarContactos(org.mockito.ArgumentMatchers.eq(captor.getValue().getId()),
                org.mockito.ArgumentMatchers.eq("bea"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(contacto));

        var resultados = servicio.buscarContactos("bea", "Bearer " + sesion.token());

        assertEquals(1, resultados.size());
        assertEquals("Beatriz", resultados.get(0).nombre());
        assertEquals(null, resultados.get(0).avatar());
    }

    @Test
    void limitaBusquedasPorUsuarioParaDificultarLaEnumeracion() {
        when(usuarios.findByCorreo("ana@example.com")).thenReturn(Optional.empty());
        when(usuarios.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var sesion = servicio.registrar(new RegistroRequest("Ana", "ana@example.com", "secreto123"));
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        when(usuarios.buscarContactos(org.mockito.ArgumentMatchers.eq(captor.getValue().getId()),
                org.mockito.ArgumentMatchers.eq("ana"), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        for (int i = 0; i < 12; i++) servicio.buscarContactos("ana", "Bearer " + sesion.token());
        ResponseStatusException limite = assertThrows(ResponseStatusException.class,
                () -> servicio.buscarContactos("ana", "Bearer " + sesion.token()));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, limite.getStatusCode());
        verify(usuarios, times(12)).buscarContactos(org.mockito.ArgumentMatchers.eq(captor.getValue().getId()),
                org.mockito.ArgumentMatchers.eq("ana"), org.mockito.ArgumentMatchers.any());
    }
}
