package com.syncchat.mensajes.configuracion;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "chat.events";
    public static final String ROUTING_KEY = "chat.message.created";
    @Bean
    TopicExchange chatEventsExchange() { return new TopicExchange(EXCHANGE); }
    @Bean
    MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}
