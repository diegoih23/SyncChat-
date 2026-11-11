package com.syncchat.mensajes.eventos;

import com.syncchat.mensajes.configuracion.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class MensajeEventPublisher {
    private final RabbitTemplate rabbit;
    public MensajeEventPublisher(RabbitTemplate rabbit) { this.rabbit = rabbit; }
    public void publicar(MensajeCreado evento) {
        // El evento es un contrato JSON compartido, no una clase que realtime deba cargar.
        rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, evento, mensaje -> {
            mensaje.getMessageProperties().getHeaders().remove("__TypeId__");
            return mensaje;
        });
    }
}
