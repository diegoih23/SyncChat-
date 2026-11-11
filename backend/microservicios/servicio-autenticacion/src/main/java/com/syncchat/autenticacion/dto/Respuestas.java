package com.syncchat.autenticacion.dto;
public final class Respuestas {
    private Respuestas() {}
    public record Autenticacion(String token, UsuarioResponse usuario) {}
    public record UsuarioResponse(String id, String nombre, String correo) {}
    public record ContactoResponse(String id, String nombre) {}
    public record ContactoPublicoResponse(String nombre, String avatar) {}
    public record EmailStatus(boolean registrado) {}
    public record Recuperacion(String mensaje, String token) {}
    public record Mensaje(String mensaje) {}
}
