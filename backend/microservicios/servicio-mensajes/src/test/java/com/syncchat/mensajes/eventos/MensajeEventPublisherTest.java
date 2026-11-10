package com.syncchat.mensajes.eventos;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.syncchat.mensajes.configuracion.RabbitConfig;

class MensajeEventPublisherTest {
    @Test
    void publicaEventoEnExchangeYRoutingKeyCorrectos() {
        RabbitTemplate rabbit = org.mockito.Mockito.mock(RabbitTemplate.class);
        MensajeEventPublisher publisher = new MensajeEventPublisher(rabbit);
        MensajeCreado evento = new MensajeCreado(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Hola", Instant.parse("2026-01-01T12:00:00Z"));
        publisher.publicar(evento);
        verify(rabbit).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY), same(evento));
    }
}
