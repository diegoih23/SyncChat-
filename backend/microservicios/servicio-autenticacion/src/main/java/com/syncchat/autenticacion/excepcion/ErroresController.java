package com.syncchat.autenticacion.excepcion;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ErroresController {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos", campos);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, Object>> negocio(ResponseStatusException ex) {
        return respuesta(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason(), null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> jsonInvalido() {
        return respuesta(HttpStatus.BAD_REQUEST, "Revisa el formato de los datos enviados", null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Map<String, Object>> parametrosInvalidos() {
        return respuesta(HttpStatus.BAD_REQUEST, "Revisa los datos ingresados", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> inesperado() {
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible completar la operación", null);
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String mensaje, Object detalles) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", Instant.now()); cuerpo.put("status", status.value());
        cuerpo.put("error", status.getReasonPhrase()); cuerpo.put("mensaje", mensaje);
        if (detalles != null) cuerpo.put("campos", detalles);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
