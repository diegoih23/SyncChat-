package com.syncchat.gateway.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

class JwtGatewayFilterTest {
    @Test
    void rechazaRutaProtegidaSinToken() {
        JwtGatewayFilter filtro = new JwtGatewayFilter("syncchat-test-secret-key-with-at-least-32-bytes");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/mensajes").build());

        filtro.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(chain);
    }
}
