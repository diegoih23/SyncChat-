package com.syncchat.autenticacion.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record RecuperarRequest(@NotBlank(message = "El correo es obligatorio") @Email(message = "Ingresa un correo válido") @Size(max = 180) String correo) {}
