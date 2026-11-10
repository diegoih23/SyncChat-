package com.syncchat.autenticacion.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    private UUID id;
    @Column(nullable = false, length = 120)
    private String nombre;
    @Column(nullable = false, unique = true, length = 180)
    private String correo;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;
    @Column(name = "reset_token_hash", length = 255)
    private String resetTokenHash;
    @Column(name = "reset_token_expira")
    private Instant resetTokenExpira;

    protected Usuario() {}

    public Usuario(UUID id, String nombre, String correo, String passwordHash, Instant creadoEn) {
        this.id = id; this.nombre = nombre; this.correo = correo;
        this.passwordHash = passwordHash; this.creadoEn = creadoEn;
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getPasswordHash() { return passwordHash; }
    public String getResetTokenHash() { return resetTokenHash; }
    public Instant getResetTokenExpira() { return resetTokenExpira; }
    public void cambiarPassword(String passwordHash) { this.passwordHash = passwordHash; }
    public void asignarTokenRecuperacion(String hash, Instant expira) {
        this.resetTokenHash = hash; this.resetTokenExpira = expira;
    }
    public void limpiarTokenRecuperacion() { this.resetTokenHash = null; this.resetTokenExpira = null; }
}
