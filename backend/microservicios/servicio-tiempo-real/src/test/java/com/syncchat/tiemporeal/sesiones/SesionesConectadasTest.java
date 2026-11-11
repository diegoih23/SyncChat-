package com.syncchat.tiemporeal.sesiones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

class SesionesConectadasTest {
    @Test
    void agregaYEliminaSesionesPorUsuario() {
        SesionesConectadas sesiones = new SesionesConectadas();
        WebSocketSession session = mock(WebSocketSession.class);
        sesiones.agregar("usuario-1", session);
        assertEquals(1, sesiones.total());
        sesiones.quitar("usuario-1", session);
        assertEquals(0, sesiones.total());
    }
}
