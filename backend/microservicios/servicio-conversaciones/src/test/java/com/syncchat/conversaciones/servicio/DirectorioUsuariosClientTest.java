package com.syncchat.conversaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class DirectorioUsuariosClientTest {
    @Test
    void resuelveNombreConJwtYCredencialInternaSinNecesitarExponerElIdAlCliente() {
        UUID id = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DirectorioUsuariosClient cliente = new DirectorioUsuariosClient(builder,
                "http://autenticacion/api/auth/usuarios/buscar", "http://autenticacion/api/auth/usuarios", "secreto-interno");
        server.expect(requestTo("http://autenticacion/api/auth/usuarios/" + id + "/interno"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer usuario"))
                .andExpect(header("X-SyncChat-Service-Token", "secreto-interno"))
                .andRespond(withSuccess("{\"id\":\"" + id + "\",\"nombre\":\"Ana\"}", MediaType.APPLICATION_JSON));

        var contacto = cliente.buscarPorId(id, "Bearer usuario");

        assertThat(contacto.nombre()).isEqualTo("Ana");
        assertThat(contacto.id()).isEqualTo(id.toString());
        server.verify();
    }

    @Test
    void noOcultaErroresDelDirectorioComoSiElContactoExistiera() {
        UUID id = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DirectorioUsuariosClient cliente = new DirectorioUsuariosClient(builder,
                "http://autenticacion/api/auth/usuarios/buscar", "http://autenticacion/api/auth/usuarios", "secreto-interno");
        server.expect(requestTo("http://autenticacion/api/auth/usuarios/" + id + "/interno"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(
                        org.springframework.http.HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> cliente.buscarPorId(id, "Bearer usuario"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No se encontró una cuenta participante");
        server.verify();
    }
}
