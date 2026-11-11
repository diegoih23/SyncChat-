package com.syncchat.conversaciones.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InvitacionRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Ingresa un correo válido")
        String correo) {}
