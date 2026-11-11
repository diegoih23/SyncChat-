package com.syncchat.mensajes.eventos;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.mockito.ArgumentCaptor;
import com.syncchat.mensajes.configuracion.RabbitConfig;

class MensajeEventPublisherTest {
    @Test
    void publicaEventoEnExchangeYRoutingKeyCorrectos() {
        RabbitTemplate rabbit = org.mockito.Mockito.mock(RabbitTemplate.class);
        MensajeEventPublisher publisher = new MensajeEventPublisher(rabbit);
        MensajeCreado evento = new MensajeCreado(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Ana",
                List.of(UUID.randomUUID()), "Hola", Instant.parse("2026-01-01T12:00:00Z"));
        publisher.publicar(evento);
        ArgumentCaptor<MessagePostProcessor> processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbit).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY), same(evento), processor.capture());
        Message raw = new Message("{}".getBytes(), new org.springframework.amqp.core.MessageProperties());
        raw.getMessageProperties().getHeaders().put("__TypeId__", "com.syncchat.mensajes.eventos.MensajeCreado");
        processor.getValue().postProcessMessage(raw);
        org.assertj.core.api.Assertions.assertThat(raw.getMessageProperties().getHeaders()).doesNotContainKey("__TypeId__");
    }
}
