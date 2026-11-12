package com.syncchat.tiemporeal.eventos;

import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncchat.tiemporeal.sesiones.SesionesConectadas;

class MensajesListenerTest {
    @Test
    void aceptaTimestampNumericoDelContratoJsonCompartido() throws Exception {
        var json = new ObjectMapper().readTree("{\"id\":\"m\",\"remitenteNombre\":\"Ana\",\"destinatarios\":[],\"creadoEn\":1794380438.926345}");
        org.junit.jupiter.api.Assertions.assertEquals(java.time.Instant.parse("2026-11-11T07:00:38.926345Z"),
                MensajeCreado.desde(json).creadoEn());
        org.junit.jupiter.api.Assertions.assertEquals("Ana", MensajeCreado.desde(json).remitenteNombre());
    }

    @Test
    void entregaGrupoATodasLasSesionesAutorizadasYDescartaDuplicados() throws Exception {
        SesionesConectadas sesiones = new SesionesConectadas();
        String miembroA = "33333333-3333-3333-3333-333333333333";
        String miembroB = "55555555-5555-5555-5555-555555555555";
        WebSocketSession sesionA1 = sesionAbierta();
        WebSocketSession sesionA2 = sesionAbierta();
        WebSocketSession sesionB = sesionAbierta();
        WebSocketSession ajeno = sesionAbierta();
        sesiones.agregar(miembroA, sesionA1);
        sesiones.agregar(miembroA, sesionA2);
        sesiones.agregar(miembroB, sesionB);
        sesiones.agregar("66666666-6666-6666-6666-666666666666", ajeno);
        MensajesListener listener = new MensajesListener(new ObjectMapper(), sesiones);

        Message evento = evento(List.of(miembroA, miembroB));
        listener.recibir(evento);
        listener.recibir(evento);

        verify(sesionA1, times(1)).sendMessage(any());
        verify(sesionA2, times(1)).sendMessage(any());
        verify(sesionB, times(1)).sendMessage(any());
        verifyNoInteractions(ajeno);
    }

    @Test
    void unaSesionQueFallaNoImpideEntregarAlResto() throws Exception {
        SesionesConectadas sesiones = new SesionesConectadas();
        String miembro = "33333333-3333-3333-3333-333333333333";
        WebSocketSession fallida = sesionAbierta();
        WebSocketSession sana = sesionAbierta();
        doThrow(new java.io.IOException("socket caído")).when(fallida).sendMessage(any());
        sesiones.agregar(miembro, fallida);
        sesiones.agregar(miembro, sana);

        new MensajesListener(new ObjectMapper(), sesiones).recibir(evento(List.of(miembro)));

        verify(sana).sendMessage(any());
        verify(fallida).close(any());
        org.junit.jupiter.api.Assertions.assertEquals(1, sesiones.total());
    }

    private static WebSocketSession sesionAbierta() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private static Message evento(java.util.List<String> destinatarios) {
        String lista = destinatarios.stream().map(id -> "\"" + id + "\"").collect(java.util.stream.Collectors.joining(","));
        String json = "{\"id\":\"11111111-1111-1111-1111-111111111111\","
                + "\"conversacionId\":\"22222222-2222-2222-2222-222222222222\","
                + "\"remitenteId\":\"44444444-4444-4444-4444-444444444444\","
                + "\"remitenteNombre\":\"Ana\","
                + "\"destinatarios\":[" + lista + "],\"contenido\":\"Hola\","
                + "\"creadoEn\":\"2026-01-01T12:00:00Z\"}";
        return new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
