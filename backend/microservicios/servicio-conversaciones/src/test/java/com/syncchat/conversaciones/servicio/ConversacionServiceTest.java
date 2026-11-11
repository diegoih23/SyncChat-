package com.syncchat.conversaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.syncchat.conversaciones.dto.ConversacionRequest;
import com.syncchat.conversaciones.dto.ConversacionResponse;
import com.syncchat.conversaciones.entidad.Conversacion;
import com.syncchat.conversaciones.repositorio.ConversacionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ConversacionServiceTest {
    @Mock ConversacionRepository repositorio;
    private ConversacionService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ConversacionService(repositorio);
    }

    @Test
    void creaConversacionConNombreLimpioYUsuarioDelToken() {
        UUID usuario = UUID.randomUUID();
        when(repositorio.save(any(Conversacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConversacionResponse respuesta = servicio.crear(new ConversacionRequest("  Equipo de producto  "), usuario);

        ArgumentCaptor<Conversacion> captor = ArgumentCaptor.forClass(Conversacion.class);
        verify(repositorio).save(captor.capture());
        Conversacion guardada = captor.getValue();
        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getNombre()).isEqualTo("Equipo de producto");
        assertThat(guardada.getCreadoPor()).isEqualTo(usuario);
        assertThat(guardada.getCreadoEn()).isNotNull();
        assertThat(respuesta.id()).isEqualTo(guardada.getId().toString());
        assertThat(respuesta.nombre()).isEqualTo("Equipo de producto");
        assertThat(respuesta.creadoPor()).isEqualTo(usuario.toString());
    }

    @Test
    void listaConversacionesDelUsuarioEnElOrdenDelRepositorio() {
        UUID usuario = UUID.randomUUID();
        Conversacion reciente = new Conversacion(UUID.randomUUID(), "Reciente", usuario, Instant.parse("2026-11-11T10:00:00Z"));
        Conversacion anterior = new Conversacion(UUID.randomUUID(), "Anterior", usuario, Instant.parse("2026-11-10T10:00:00Z"));
        when(repositorio.findByCreadoPorOrderByCreadoEnDesc(usuario)).thenReturn(List.of(reciente, anterior));

        List<ConversacionResponse> respuesta = servicio.listar(usuario);

        assertThat(respuesta).extracting(ConversacionResponse::nombre)
                .containsExactly("Reciente", "Anterior");
        assertThat(respuesta).extracting(ConversacionResponse::id)
                .containsExactly(reciente.getId().toString(), anterior.getId().toString());
        verify(repositorio).findByCreadoPorOrderByCreadoEnDesc(usuario);
    }

    @Test
    void obtieneConversacionExistente() {
        UUID usuario = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Conversacion conversacion = new Conversacion(id, "General", usuario, Instant.parse("2026-11-11T10:00:00Z"));
        when(repositorio.findById(id)).thenReturn(Optional.of(conversacion));

        ConversacionResponse respuesta = servicio.obtener(id);

        assertThat(respuesta.id()).isEqualTo(id.toString());
        assertThat(respuesta.nombre()).isEqualTo("General");
        assertThat(respuesta.creadoPor()).isEqualTo(usuario.toString());
    }

    @Test
    void fallaAlObtenerUnaConversacionInexistente() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(id))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND))
                .hasMessageContaining("La conversación no existe");
    }

    @Test
    void eliminaLaConversacionCuandoElUsuarioEsElCreador() {
        UUID usuario = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Conversacion conversacion = new Conversacion(id, "Temporal", usuario, Instant.now());
        when(repositorio.findById(id)).thenReturn(Optional.of(conversacion));

        servicio.eliminar(id, usuario);

        verify(repositorio).delete(conversacion);
    }

    @Test
    void rechazaEliminarUnaConversacionDeOtroUsuario() {
        UUID creador = UUID.randomUUID();
        UUID otroUsuario = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Conversacion conversacion = new Conversacion(id, "Privada", creador, Instant.now());
        when(repositorio.findById(id)).thenReturn(Optional.of(conversacion));

        assertThatThrownBy(() -> servicio.eliminar(id, otroUsuario))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN))
                .hasMessageContaining("Solo quien creó la conversación puede eliminarla");
        verify(repositorio, never()).delete(any(Conversacion.class));
    }

    @Test
    void fallaAlEliminarUnaConversacionInexistente() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(id, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(repositorio, never()).delete(any(Conversacion.class));
    }
}
