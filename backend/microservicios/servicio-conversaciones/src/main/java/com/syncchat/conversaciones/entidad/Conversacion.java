package com.syncchat.conversaciones.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversaciones")
public class Conversacion {
    @Id
    private UUID id;
    @Column(nullable = false, length = 160)
    private String nombre;
    @Column(name = "creado_por", nullable = false)
    private UUID creadoPor;
    @Column(name = "destinatario_id")
    private UUID destinatarioId;
    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected Conversacion() {}

    public Conversacion(UUID id, String nombre, UUID creadoPor, Instant creadoEn) {
        this.id = id;
        this.nombre = nombre;
        this.creadoPor = creadoPor;
        this.creadoEn = creadoEn;
    }

    public Conversacion(UUID id, String nombre, UUID creadoPor, UUID destinatarioId, Instant creadoEn) {
        this(id, nombre, creadoPor, creadoEn);
        this.destinatarioId = destinatarioId;
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public UUID getCreadoPor() { return creadoPor; }
    public UUID getDestinatarioId() { return destinatarioId; }
    public Instant getCreadoEn() { return creadoEn; }
}
