package com.syncchat.mensajes.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MensajeServicioTest {
    @Mock MensajeRepositorio repositorio;

    @Test
    void reintentoConLaMismaClaveReutilizaMensajePersistido() {
        UUID clave = UUID.randomUUID();
        UUID conversacion = UUID.randomUUID();
        UUID remitente = UUID.randomUUID();
        Mensaje guardado = new Mensaje(clave, conversacion, remitente, "hola", OffsetDateTime.now());
        when(repositorio.buscarPorId(clave)).thenReturn(Optional.of(guardado));

        Mensaje resultado = new MensajeServicio(repositorio).crear(
                new CrearMensaje(conversacion, remitente, " hola ", clave));

        assertEquals(guardado, resultado);
        verify(repositorio, never()).guardar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rechazaReutilizarLaClaveParaOtroMensaje() {
        UUID clave = UUID.randomUUID();
        when(repositorio.buscarPorId(clave)).thenReturn(Optional.of(new Mensaje(
                clave, UUID.randomUUID(), UUID.randomUUID(), "otro", OffsetDateTime.now())));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> new MensajeServicio(repositorio).crear(new CrearMensaje(
                        UUID.randomUUID(), UUID.randomUUID(), "contenido", clave)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repositorio, never()).guardar(org.mockito.ArgumentMatchers.any());
    }
}
