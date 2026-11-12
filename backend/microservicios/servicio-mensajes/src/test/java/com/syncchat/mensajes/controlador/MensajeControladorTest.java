package com.syncchat.mensajes.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

import com.syncchat.mensajes.eventos.MensajeEventPublisher;
import com.syncchat.mensajes.modelo.CrearMensaje;
import com.syncchat.mensajes.modelo.Mensaje;
import com.syncchat.mensajes.modelo.MensajeServicio;
import com.syncchat.mensajes.seguridad.ConversacionesPermisosClient;
import com.syncchat.mensajes.seguridad.TokenService;
import com.syncchat.mensajes.vista.CrearMensajeVista;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MensajeControladorTest {
    @Mock MensajeServicio servicio;
    @Mock MensajeEventPublisher publisher;
    @Mock TokenService tokens;
    @Mock ConversacionesPermisosClient permisos;
    @InjectMocks MensajeControlador controlador;

    @Test
    void tomaRemitenteDelJwtYEntregaSoloAIntegrantesAceptados() {
        UUID conversacionId = UUID.randomUUID();
        UUID usuarioAutenticado = UUID.randomUUID();
        UUID otroParticipante = UUID.randomUUID();
        String authorization = "Bearer token";
        when(tokens.usuarioDesdeHeader(authorization)).thenReturn(usuarioAutenticado);
        when(permisos.participantesAceptados(conversacionId, authorization))
                .thenReturn(List.of(
                        new ConversacionesPermisosClient.ParticipanteAceptado(usuarioAutenticado, "Ana"),
                        new ConversacionesPermisosClient.ParticipanteAceptado(otroParticipante, "Beto")));
        when(servicio.crear(any())).thenAnswer(invocation -> {
            CrearMensaje command = invocation.getArgument(0);
            return new Mensaje(command.idempotencyKey(), command.conversacionId(), command.remitenteId(), command.contenido(),
                    OffsetDateTime.now());
        });
        UUID clave = UUID.randomUUID();
        CrearMensajeVista vista = new CrearMensajeVista(conversacionId, clave, "Hola");

        var respuesta = controlador.crear(authorization, vista);

        assertEquals(201, respuesta.getStatusCode().value());
        ArgumentCaptor<CrearMensaje> command = ArgumentCaptor.forClass(CrearMensaje.class);
        verify(servicio).crear(command.capture());
        assertEquals(usuarioAutenticado, command.getValue().remitenteId());
        assertEquals(clave, command.getValue().idempotencyKey());
        assertEquals("Ana", respuesta.getBody().remitenteNombre());
        verify(permisos).participantesAceptados(conversacionId, authorization);
        verify(publisher).publicar(org.mockito.ArgumentMatchers.argThat(evento ->
                evento.destinatarios().equals(List.of(usuarioAutenticado, otroParticipante))
                        && "Ana".equals(evento.remitenteNombre())));
        var orden = inOrder(servicio, publisher);
        orden.verify(servicio).crear(any());
        orden.verify(publisher).publicar(any());
    }

    @Test
    void usuarioAjenoNoPuedePersistirNiPublicarMensajes() {
        UUID conversacionId = UUID.randomUUID();
        String authorization = "Bearer token-ajeno";
        when(tokens.usuarioDesdeHeader(authorization)).thenReturn(UUID.randomUUID());
        when(permisos.participantesAceptados(conversacionId, authorization))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
                () -> controlador.crear(authorization,
                        new CrearMensajeVista(conversacionId, UUID.randomUUID(), "No autorizado")));

        verifyNoInteractions(servicio, publisher);
    }
}
