package com.syncchat.conversaciones.repositorio;

import com.syncchat.conversaciones.entidad.Conversacion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversacionRepository extends JpaRepository<Conversacion, UUID> {
}
