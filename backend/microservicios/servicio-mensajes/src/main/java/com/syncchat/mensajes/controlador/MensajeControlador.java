package com.syncchat.mensajes.controlador;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.syncchat.mensajes.modelo.MensajeServicio;
import com.syncchat.mensajes.vista.CrearMensajeVista;
import com.syncchat.mensajes.vista.MensajeVista;

/**
 * Controlador MVC: gestiona las peticiones HTTP del microservicio.
 * No contiene lógica de negocio ni acceso a datos: recibe vistas,
 * delega en el servicio del modelo y responde con vistas.
 */
@RestController
@RequestMapping("/api/mensajes")
public class MensajeControlador {
    private final MensajeServicio servicio;

    public MensajeControlador(MensajeServicio servicio) { this.servicio = servicio; }

    @PostMapping
    public ResponseEntity<MensajeVista> crear(@RequestBody CrearMensajeVista nuevo) {
        MensajeVista mensaje = MensajeVista.desdeModelo(servicio.crear(nuevo.aModelo()));
        return ResponseEntity.created(URI.create("/api/mensajes/" + mensaje.id())).body(mensaje);
    }

    @GetMapping("/conversacion/{id}")
    public List<MensajeVista> historial(@PathVariable UUID id) {
        return servicio.historial(id).stream().map(MensajeVista::desdeModelo).toList();
    }
}
