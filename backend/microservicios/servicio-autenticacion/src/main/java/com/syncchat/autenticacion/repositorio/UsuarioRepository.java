package com.syncchat.autenticacion.repositorio;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import com.syncchat.autenticacion.entidad.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByCorreo(String correo);

    @Query("select u from Usuario u where u.id <> :usuarioId and "
            + "(lower(u.nombre) like lower(concat('%', :consulta, '%')) "
            + "or lower(u.correo) like lower(concat('%', :consulta, '%'))) order by u.nombre")
    List<Usuario> buscarContactos(@Param("usuarioId") UUID usuarioId,
                                  @Param("consulta") String consulta, Pageable pageable);
}
