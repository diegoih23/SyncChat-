package com.syncchat.autenticacion.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record RestablecerRequest(@NotBlank(message = "El correo es obligatorio") @Email(message = "Ingresa un correo válido") String correo, @NotBlank(message = "El código de recuperación es obligatorio") String token, @NotBlank(message = "La contraseña es obligatoria") @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password) {}
