package com.syncchat.conversaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.syncchat.conversaciones.dto.ConversacionRequest;
import com.syncchat.conversaciones.dto.ConversacionResponse;
import com.syncchat.conversaciones.dto.ParticipanteResponse;
import com.syncchat.conversaciones.dto.ParticipanteAceptadoResponse;
import com.syncchat.conversaciones.entidad.Conversacion;
import com.syncchat.conversaciones.entidad.ConversacionParticipante;
import com.syncchat.conversaciones.entidad.EstadoParticipante;
import com.syncchat.conversaciones.repositorio.ConversacionRepository;
import com.syncchat.conversaciones.repositorio.ConversacionParticipanteRepository;
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
    @Mock ConversacionParticipanteRepository participantes;
    @Mock DirectorioUsuariosClient directorio;
    private ConversacionService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ConversacionService(repositorio, participantes, directorio);
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
        ArgumentCaptor<ConversacionParticipante> participante = ArgumentCaptor.forClass(ConversacionParticipante.class);
        verify(participantes).save(participante.capture());
        assertThat(participante.getValue().getUsuarioId()).isEqualTo(usuario);
        assertThat(participante.getValue().getEstado()).isEqualTo(EstadoParticipante.ACCEPTED);
        assertThat(participante.getValue().isAdministrador()).isTrue();
    }

    @Test
    void listaConversacionesDelUsuarioEnElOrdenDelRepositorio() {
        UUID usuario = UUID.randomUUID();
        Conversacion reciente = new Conversacion(UUID.randomUUID(), "Reciente", usuario, Instant.parse("2026-11-11T10:00:00Z"));
        Conversacion anterior = new Conversacion(UUID.randomUUID(), "Anterior", usuario, Instant.parse("2026-11-10T10:00:00Z"));
        when(participantes.findByUsuarioIdAndEstado(usuario, EstadoParticipante.ACCEPTED)).thenReturn(List.of(
                new ConversacionParticipante(UUID.randomUUID(), reciente.getId(), usuario, EstadoParticipante.ACCEPTED, false, Instant.now()),
                new ConversacionParticipante(UUID.randomUUID(), anterior.getId(), usuario, EstadoParticipante.ACCEPTED, false, Instant.now())));
        when(repositorio.findAllById(List.of(reciente.getId(), anterior.getId()))).thenReturn(List.of(reciente, anterior));

        List<ConversacionResponse> respuesta = servicio.listar(usuario);

        assertThat(respuesta).extracting(ConversacionResponse::nombre)
                .containsExactly("Reciente", "Anterior");
        assertThat(respuesta).extracting(ConversacionResponse::id)
                .containsExactly(reciente.getId().toString(), anterior.getId().toString());
        verify(participantes).findByUsuarioIdAndEstado(usuario, EstadoParticipante.ACCEPTED);
    }

    @Test
    void noListaConversacionesDondeElUsuarioSoloEstaInvitado() {
        UUID usuario = UUID.randomUUID();
        when(participantes.findByUsuarioIdAndEstado(usuario, EstadoParticipante.ACCEPTED)).thenReturn(List.of());

        assertThat(servicio.listar(usuario)).isEmpty();
        verify(repositorio).findAllById(List.of());
    }

    @Test
    void soloDevuelveDestinatariosAceptadosYRechazaAlPendiente() {
        UUID conversacionId = UUID.randomUUID();
        UUID miembro = UUID.randomUUID();
        UUID invitadoPendiente = UUID.randomUUID();
        when(participantes.findByConversacionIdAndUsuarioId(conversacionId, miembro)).thenReturn(Optional.of(
                new ConversacionParticipante(UUID.randomUUID(), conversacionId, miembro,
                        EstadoParticipante.ACCEPTED, false, Instant.now())));
        when(participantes.findByConversacionIdAndEstado(conversacionId, EstadoParticipante.ACCEPTED)).thenReturn(List.of(
                new ConversacionParticipante(UUID.randomUUID(), conversacionId, miembro,
                        EstadoParticipante.ACCEPTED, false, Instant.now())));

        assertThat(servicio.destinatariosAceptados(conversacionId, miembro)).containsExactly(miembro);
        assertThatThrownBy(() -> servicio.destinatariosAceptados(conversacionId, invitadoPendiente))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No eres participante");
        verify(participantes).findByConversacionIdAndEstado(conversacionId, EstadoParticipante.ACCEPTED);
    }

    @Test
    void devuelveIdentidadInternaSoloDeParticipantesAceptadosParaMensajes() {
        UUID conversacionId = UUID.randomUUID();
        UUID miembro = UUID.randomUUID();
        UUID pendiente = UUID.randomUUID();
        when(participantes.findByConversacionIdAndUsuarioId(conversacionId, miembro)).thenReturn(Optional.of(
                new ConversacionParticipante(UUID.randomUUID(), conversacionId, miembro,
                        EstadoParticipante.ACCEPTED, true, Instant.now())));
        when(participantes.findByConversacionIdAndEstado(conversacionId, EstadoParticipante.ACCEPTED)).thenReturn(List.of(
                new ConversacionParticipante(UUID.randomUUID(), conversacionId, miembro,
                        EstadoParticipante.ACCEPTED, true, Instant.now())));
        when(directorio.buscarPorId(miembro, "Bearer jwt"))
                .thenReturn(new DirectorioUsuariosClient.Contacto(miembro.toString(), "Ana"));

        List<ParticipanteAceptadoResponse> resultado =
                servicio.participantesAceptadosConNombre(conversacionId, miembro, "Bearer jwt");

        assertThat(resultado).containsExactly(new ParticipanteAceptadoResponse(miembro, "Ana"));
        verify(directorio, never()).buscarPorId(pendiente, "Bearer jwt");
        assertThatThrownBy(() -> servicio.participantesAceptadosConNombre(conversacionId, pendiente, "Bearer jwt"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No eres participante");
    }

    @Test
    void listaNombresYEstadosSinExponerIdsYRequiereSerAceptado() {
        UUID conversacionId = UUID.randomUUID();
        UUID miembro = UUID.randomUUID();
        UUID invitado = UUID.randomUUID();
        when(participantes.findByConversacionIdAndUsuarioId(conversacionId, miembro)).thenReturn(Optional.of(
                new ConversacionParticipante(UUID.randomUUID(), conversacionId, miembro,
                        EstadoParticipante.ACCEPTED, true, Instant.now())));
        when(participantes.findByConversacionId(conversacionId)).thenReturn(List.of(
                new ConversacionParticipante(UUID.randomUUID(), conversacionId, miembro,
                        EstadoParticipante.ACCEPTED, true, Instant.now()),
                new ConversacionParticipante(UUID.randomUUID(), conversacionId, invitado,
                        EstadoParticipante.PENDING, false, Instant.now()),
                new ConversacionParticipante(UUID.randomUUID(), conversacionId, UUID.randomUUID(),
                        EstadoParticipante.REMOVED, false, Instant.now())));
        when(directorio.buscarPorId(miembro, "Bearer jwt"))
                .thenReturn(new DirectorioUsuariosClient.Contacto(miembro.toString(), "Ana"));
        when(directorio.buscarPorId(invitado, "Bearer jwt"))
                .thenReturn(new DirectorioUsuariosClient.Contacto(invitado.toString(), "Beto"));

        List<ParticipanteResponse> resultado = servicio.listarParticipantes(conversacionId, miembro, "Bearer jwt");

        assertThat(resultado).containsExactly(
                new ParticipanteResponse("Ana", "ACCEPTED", true),
                new ParticipanteResponse("Beto", "PENDING", false));
        assertThat(resultado.toString()).doesNotContain(miembro.toString(), invitado.toString());
    }

    @Test
    void soloUnAdministradorAceptadoPuedeInvitar() {
        UUID conversacionId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID invitadoId = UUID.randomUUID();
        when(repositorio.findById(conversacionId)).thenReturn(Optional.of(
                new Conversacion(conversacionId, "Grupo", adminId, Instant.now())));
        when(participantes.findByConversacionIdAndUsuarioId(conversacionId, adminId)).thenReturn(Optional.of(
                new ConversacionParticipante(UUID.randomUUID(), conversacionId, adminId,
                        EstadoParticipante.ACCEPTED, true, Instant.now())));
        when(participantes.findByConversacionIdAndUsuarioId(conversacionId, invitadoId)).thenReturn(Optional.empty());

        servicio.invitar(conversacionId, adminId, invitadoId);

        verify(participantes).save(org.mockito.ArgumentMatchers.argThat(p -> p.getUsuarioId().equals(invitadoId)
                && p.getEstado() == EstadoParticipante.PENDING && !p.isAdministrador()));
    }

    @Test
    void bloqueaInvitacionesHechasPorUnMiembroSinPrivilegios() {
        UUID conversacionId = UUID.randomUUID();
        UUID miembroId = UUID.randomUUID();
        when(repositorio.findById(conversacionId)).thenReturn(Optional.of(
                new Conversacion(conversacionId, "Grupo", UUID.randomUUID(), Instant.now())));
        when(participantes.findByConversacionIdAndUsuarioId(conversacionId, miembroId)).thenReturn(Optional.of(
                new ConversacionParticipante(UUID.randomUUID(), conversacionId, miembroId,
                        EstadoParticipante.ACCEPTED, false, Instant.now())));

        assertThatThrownBy(() -> servicio.invitar(conversacionId, miembroId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
        verify(participantes, never()).save(any(ConversacionParticipante.class));
    }

    @Test
    void impideQueElUltimoAdministradorAbandoneElGrupo() {
        UUID conversacionId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        ConversacionParticipante admin = new ConversacionParticipante(UUID.randomUUID(), conversacionId,
                adminId, EstadoParticipante.ACCEPTED, true, Instant.now());
        when(participantes.findByConversacionIdAndUsuarioId(conversacionId, adminId)).thenReturn(Optional.of(admin));
        when(participantes.findByConversacionId(conversacionId)).thenReturn(List.of(admin));

        assertThatThrownBy(() -> servicio.salir(conversacionId, adminId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("último administrador");
    }

    @Test
    void resuelveElCorreoAntesDeCrearLaInvitacion() {
        UUID conversacionId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID invitadoId = UUID.randomUUID();
        when(directorio.buscarPorCorreo("ana@example.com", "Bearer token"))
                .thenReturn(new DirectorioUsuariosClient.Contacto(invitadoId.toString(), "Ana"));
        when(repositorio.findById(conversacionId)).thenReturn(Optional.of(
                new Conversacion(conversacionId, "Equipo", adminId, Instant.now())));
        when(participantes.findByConversacionIdAndUsuarioId(conversacionId, adminId)).thenReturn(Optional.of(
                new ConversacionParticipante(UUID.randomUUID(), conversacionId, adminId,
                        EstadoParticipante.ACCEPTED, true, Instant.now())));
        when(participantes.findByConversacionIdAndUsuarioId(conversacionId, invitadoId)).thenReturn(Optional.empty());

        servicio.invitarPorCorreo(conversacionId, adminId, "ana@example.com", "Bearer token");

        verify(directorio).buscarPorCorreo("ana@example.com", "Bearer token");
        verify(participantes).save(org.mockito.ArgumentMatchers.argThat(p -> p.getUsuarioId().equals(invitadoId)
                && p.getEstado() == EstadoParticipante.PENDING));
    }

    @Test
    void cambiaInvitacionPendienteARechazada() {
        UUID conversacionId = UUID.randomUUID();
        UUID invitadoId = UUID.randomUUID();
        ConversacionParticipante invitado = new ConversacionParticipante(UUID.randomUUID(), conversacionId,
                invitadoId, EstadoParticipante.PENDING, false, Instant.now());
        when(participantes.findByConversacionIdAndUsuarioId(conversacionId, invitadoId)).thenReturn(Optional.of(invitado));

        servicio.responderInvitacion(conversacionId, invitadoId, false);

        assertThat(invitado.getEstado()).isEqualTo(EstadoParticipante.DECLINED);
        verify(participantes).save(invitado);
    }

    @Test
    void listaSoloInvitacionesPendientesDelUsuario() {
        UUID usuarioId = UUID.randomUUID();
        UUID conversacionId = UUID.randomUUID();
        Instant invitadaEn = Instant.now();
        when(participantes.findByUsuarioIdAndEstado(usuarioId, EstadoParticipante.PENDING)).thenReturn(List.of(
                new ConversacionParticipante(UUID.randomUUID(), conversacionId, usuarioId,
                        EstadoParticipante.PENDING, false, invitadaEn)));
        when(repositorio.findById(conversacionId)).thenReturn(Optional.of(
                new Conversacion(conversacionId, "Proyecto", UUID.randomUUID(), invitadaEn)));

        var pendientes = servicio.invitacionesPendientes(usuarioId);

        assertThat(pendientes).hasSize(1);
        assertThat(pendientes.get(0).nombre()).isEqualTo("Proyecto");
        assertThat(pendientes.get(0).conversacionId()).isEqualTo(conversacionId.toString());
    }

    @Test
    void obtieneConversacionExistente() {
        UUID usuario = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Conversacion conversacion = new Conversacion(id, "General", usuario, Instant.parse("2026-11-11T10:00:00Z"));
        when(participantes.findByConversacionIdAndUsuarioId(id, usuario)).thenReturn(Optional.of(
                new ConversacionParticipante(UUID.randomUUID(), id, usuario, EstadoParticipante.ACCEPTED, false, Instant.now())));
        when(repositorio.findById(id)).thenReturn(Optional.of(conversacion));

        ConversacionResponse respuesta = servicio.obtener(id, usuario);

        assertThat(respuesta.id()).isEqualTo(id.toString());
        assertThat(respuesta.nombre()).isEqualTo("General");
        assertThat(respuesta.creadoPor()).isEqualTo(usuario.toString());
    }

    @Test
    void bloqueaElAccesoAUnInvitadoPendiente() {
        UUID id = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        when(participantes.findByConversacionIdAndUsuarioId(id, usuario)).thenReturn(Optional.of(
                new ConversacionParticipante(UUID.randomUUID(), id, usuario, EstadoParticipante.PENDING, false, Instant.now())));

        assertThatThrownBy(() -> servicio.obtener(id, usuario))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN))
                .hasMessageContaining("Debes aceptar la invitación");
        verify(repositorio, never()).findById(id);
    }

    @Test
    void fallaAlObtenerUnaConversacionInexistente() {
        UUID id = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        when(participantes.findByConversacionIdAndUsuarioId(id, usuario)).thenReturn(Optional.of(
                new ConversacionParticipante(UUID.randomUUID(), id, usuario, EstadoParticipante.ACCEPTED, false, Instant.now())));
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(id, usuario))
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
        when(participantes.findByConversacionIdAndUsuarioId(id, usuario)).thenReturn(Optional.of(
                new ConversacionParticipante(UUID.randomUUID(), id, usuario, EstadoParticipante.ACCEPTED, true, Instant.now())));
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
        when(participantes.findByConversacionIdAndUsuarioId(id, otroUsuario)).thenReturn(Optional.of(
                new ConversacionParticipante(UUID.randomUUID(), id, otroUsuario, EstadoParticipante.ACCEPTED, false, Instant.now())));
        when(repositorio.findById(id)).thenReturn(Optional.of(conversacion));

        assertThatThrownBy(() -> servicio.eliminar(id, otroUsuario))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN))
                .hasMessageContaining("requiere ser administrador");
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
