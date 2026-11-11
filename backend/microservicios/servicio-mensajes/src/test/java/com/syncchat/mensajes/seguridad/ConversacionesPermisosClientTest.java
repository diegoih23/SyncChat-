package com.syncchat.mensajes.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class ConversacionesPermisosClientTest {
    @Test
    void pideAlServicioDeConversacionesLaListaAutorizadaAntesDePublicar() {
        UUID conversacion = UUID.randomUUID();
        UUID primero = UUID.randomUUID();
        UUID segundo = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ConversacionesPermisosClient cliente = new ConversacionesPermisosClient(builder,
                "http://conversaciones/api/conversaciones", "secreto-servicio");
        server.expect(requestTo("http://conversaciones/api/conversaciones/" + conversacion + "/participantes/aceptados"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer usuario"))
                .andExpect(header("X-SyncChat-Service-Token", "secreto-servicio"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "[\"" + primero + "\",\"" + segundo + "\"]", MediaType.APPLICATION_JSON));

        List<UUID> autorizados = cliente.destinatariosAceptados(conversacion, "Bearer usuario");

        assertThat(autorizados).containsExactly(primero, segundo);
        server.verify();
    }

    @Test
    void rechazaAccesoDenegadoPorElServicioDeConversaciones() {
        UUID conversacion = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ConversacionesPermisosClient cliente = new ConversacionesPermisosClient(builder,
                "http://conversaciones/api/conversaciones", "secreto-servicio");
        server.expect(requestTo("http://conversaciones/api/conversaciones/" + conversacion + "/participantes/aceptados"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(
                        org.springframework.http.HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> cliente.destinatariosAceptados(conversacion, "Bearer usuario-ajeno"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No tienes acceso a esta conversación");
        server.verify();
    }
}
