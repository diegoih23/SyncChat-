package com.syncchat.tiemporeal.configuracion;

import com.syncchat.tiemporeal.websocket.MensajesWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final MensajesWebSocketHandler handler;
    private final String[] origins;
    public WebSocketConfig(MensajesWebSocketHandler handler,
                           @Value("${syncchat.cors.allowed-origins:http://localhost:3000,http://localhost:13000,http://localhost:5173}") String allowedOrigins) {
        this.handler = handler;
        this.origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toArray(String[]::new);
    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/mensajes").setAllowedOriginPatterns(origins);
    }
}
