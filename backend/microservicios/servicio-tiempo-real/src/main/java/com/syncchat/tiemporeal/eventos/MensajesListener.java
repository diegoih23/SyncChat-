package com.syncchat.tiemporeal.eventos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncchat.tiemporeal.configuracion.RabbitConfig;
import com.syncchat.tiemporeal.sesiones.SesionesConectadas;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

@Component
public class MensajesListener {
    private final ObjectMapper mapper;
    private final SesionesConectadas sesiones;
    public MensajesListener(ObjectMapper mapper, SesionesConectadas sesiones) { this.mapper = mapper; this.sesiones = sesiones; }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void recibir(String evento) throws IOException {
        var json = mapper.readTree(evento);
        String destinatarioId = MensajeCreado.desde(json).destinatarioId();
        if (destinatarioId == null) return;
        for (var session : sesiones.delUsuario(destinatarioId)) {
            if (session.isOpen()) session.sendMessage(new TextMessage(evento));
            else sesiones.quitar(destinatarioId, session);
        }
    }
}
