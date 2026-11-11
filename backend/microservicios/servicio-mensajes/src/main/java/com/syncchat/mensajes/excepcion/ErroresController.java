package com.syncchat.mensajes.excepcion;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ErroresController {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "Revisa los datos del mensaje", campos);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> jsonInvalido() {
        return respuesta(HttpStatus.BAD_REQUEST, "Revisa el formato de los datos enviados", null);
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, Object>> estadoHttp(ResponseStatusException ex) {
        return respuesta(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason(), null);
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> inesperado() {
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible enviar el mensaje", null);
    }
    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String mensaje, Object campos) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", Instant.now()); cuerpo.put("status", status.value()); cuerpo.put("mensaje", mensaje);
        if (campos != null) cuerpo.put("campos", campos);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
