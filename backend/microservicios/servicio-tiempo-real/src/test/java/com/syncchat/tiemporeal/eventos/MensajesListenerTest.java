package com.syncchat.tiemporeal.eventos;

import static org.mockito.Mockito.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncchat.tiemporeal.sesiones.SesionesConectadas;

class MensajesListenerTest {
    @Test
    void entregaEventoAlDestinatarioConectado() throws Exception {
        SesionesConectadas sesiones = new SesionesConectadas();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        sesiones.agregar("33333333-3333-3333-3333-333333333333", session);
        MensajesListener listener = new MensajesListener(new ObjectMapper(), sesiones);

        listener.recibir("""
                {"id":"11111111-1111-1111-1111-111111111111","conversacionId":"22222222-2222-2222-2222-222222222222","remitenteId":"44444444-4444-4444-4444-444444444444","destinatarioId":"33333333-3333-3333-3333-333333333333","contenido":"Hola","creadoEn":"2026-01-01T12:00:00Z"}
                """);

        verify(session).sendMessage(any());
    }
}
