package com.syncchat.conversaciones.entidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversacionParticipanteTest {
    @Test
    void aceptaLaRespuestaYActualizaLaMarcaDeTiempo() {
        Instant inicio = Instant.parse("2026-11-11T10:00:00Z");
        Instant respuesta = inicio.plusSeconds(30);
        ConversacionParticipante participante = new ConversacionParticipante(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), EstadoParticipante.PENDING, false, inicio);

        participante.cambiarEstado(EstadoParticipante.ACCEPTED, respuesta);

        assertThat(participante.getEstado()).isEqualTo(EstadoParticipante.ACCEPTED);
        assertThat(participante.getActualizadoEn()).isEqualTo(respuesta);
    }

    @Test
    void permiteReinvitarEstadosTerminalesPeroNoConservaPrivilegios() {
        for (EstadoParticipante terminal : new EstadoParticipante[] {
                EstadoParticipante.DECLINED, EstadoParticipante.LEFT, EstadoParticipante.REMOVED }) {
            ConversacionParticipante participante = new ConversacionParticipante(UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID(), terminal, true, Instant.now());

            participante.cambiarEstado(EstadoParticipante.PENDING, Instant.now());

            assertThat(participante.getEstado()).isEqualTo(EstadoParticipante.PENDING);
            assertThat(participante.isAdministrador()).isFalse();
        }
    }

    @Test
    void rechazaTransicionesQueOmitenLaRespuestaDeInvitacion() {
        ConversacionParticipante participante = new ConversacionParticipante(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), EstadoParticipante.PENDING, false, Instant.now());

        assertThatThrownBy(() -> participante.cambiarEstado(EstadoParticipante.LEFT, Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transición de participante no permitida");
    }
}
