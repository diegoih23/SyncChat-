package com.syncchat.mensajes.modelo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Repositorio del modelo: encapsula el acceso a la base de datos.
 * Única capa que conoce SQL y el mapeo de filas a objetos del modelo.
 */
@Repository
public class MensajeRepositorio {
    private final JdbcTemplate jdbc;
    private final RowMapper<Mensaje> filaMensaje = this::mapear;

    public MensajeRepositorio(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Mensaje guardar(CrearMensaje nuevo) {
        // RETURNING devuelve el registro ya guardado, incluida su fecha.
        return jdbc.queryForObject(
                "INSERT INTO mensajes (id, conversacion_id, remitente_id, contenido) VALUES (?, ?, ?, ?) RETURNING id, conversacion_id, remitente_id, contenido, creado_en",
                filaMensaje, nuevo.idempotencyKey() == null ? UUID.randomUUID() : nuevo.idempotencyKey(),
                nuevo.conversacionId(), nuevo.remitenteId(), nuevo.contenido().trim());
    }

    public Optional<Mensaje> buscarPorId(UUID id) {
        return jdbc.query("SELECT id, conversacion_id, remitente_id, contenido, creado_en FROM mensajes WHERE id = ?",
                filaMensaje, id).stream().findFirst();
    }

    public List<Mensaje> buscarPorConversacion(UUID conversacionId) {
        return jdbc.query(
                "SELECT id, conversacion_id, remitente_id, contenido, creado_en FROM mensajes WHERE conversacion_id = ? ORDER BY creado_en, id",
                filaMensaje, conversacionId);
    }

    private Mensaje mapear(ResultSet rs, int fila) throws SQLException {
        return new Mensaje(
                rs.getObject("id", UUID.class),
                rs.getObject("conversacion_id", UUID.class),
                rs.getObject("remitente_id", UUID.class),
                rs.getString("contenido"),
                rs.getObject("creado_en", OffsetDateTime.class));
    }
}
