package com.syncchat.tiemporeal.eventos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncchat.tiemporeal.configuracion.RabbitConfig;
import com.syncchat.tiemporeal.sesiones.SesionesConectadas;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MensajesListener {
    private static final Logger log = LoggerFactory.getLogger(MensajesListener.class);
    private static final long EVENTO_TTL_MS = 24 * 60 * 60 * 1000L;
    private final ObjectMapper mapper;
    private final SesionesConectadas sesiones;
    private final ConcurrentHashMap<String, Long> eventosEntregados = new ConcurrentHashMap<>();
    private final AtomicInteger eventosProcesados = new AtomicInteger();
    public MensajesListener(ObjectMapper mapper, SesionesConectadas sesiones) { this.mapper = mapper; this.sesiones = sesiones; }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void recibir(Message mensaje) throws IOException {
        String evento = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        var json = mapper.readTree(evento);
        var creado = MensajeCreado.desde(json);
        if (creado.id() == null || creado.destinatarios().isEmpty() || !registrarPrimeraEntrega(creado.id())) return;
        log.debug("Evento de mensaje recibido; destinatarios: {}, sesiones activas: {}",
                creado.destinatarios().size(), sesiones.total());
        for (String destinatarioId : creado.destinatarios()) {
            for (var session : sesiones.delUsuario(destinatarioId)) {
                if (!session.isOpen()) {
                    sesiones.quitar(destinatarioId, session);
                    continue;
                }
                try {
                    session.sendMessage(new TextMessage(evento));
                } catch (Exception ex) {
                    log.warn("No se pudo entregar el mensaje {} a una sesión; se sincronizará desde el historial", creado.id(), ex);
                    sesiones.quitar(destinatarioId, session);
                    try { session.close(CloseStatus.SERVER_ERROR); } catch (Exception cierre) {
                        log.debug("No se pudo cerrar la sesión WebSocket fallida", cierre);
                    }
                }
            }
        }
    }

    private boolean registrarPrimeraEntrega(String id) {
        long ahora = Instant.now().toEpochMilli();
        AtomicBoolean nuevaEntrega = new AtomicBoolean();
        eventosEntregados.compute(id, (clave, anterior) -> {
            if (anterior == null || ahora - anterior > EVENTO_TTL_MS) {
                nuevaEntrega.set(true);
                return ahora;
            }
            return anterior;
        });
        if (eventosProcesados.incrementAndGet() % 256 == 0) {
            eventosEntregados.entrySet().removeIf(entrada -> ahora - entrada.getValue() > EVENTO_TTL_MS);
        }
        return nuevaEntrega.get();
    }
}
