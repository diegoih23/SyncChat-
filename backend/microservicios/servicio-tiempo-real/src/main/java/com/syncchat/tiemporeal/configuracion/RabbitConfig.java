package com.syncchat.tiemporeal.configuracion;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "chat.events";
    public static final String QUEUE = "chat.message.created";
    public static final String ROUTING_KEY = "chat.message.created";

    @Bean TopicExchange mensajesExchange() { return new TopicExchange(EXCHANGE); }
    @Bean Queue mensajesQueue() { return QueueBuilder.durable(QUEUE).build(); }
    @Bean Binding mensajesBinding(Queue mensajesQueue, TopicExchange mensajesExchange) {
        return BindingBuilder.bind(mensajesQueue).to(mensajesExchange).with(ROUTING_KEY);
    }
    @Bean MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}
