package com.isil.edu.pe.spedidos.controller.dto;

import java.time.LocalDateTime;

/**
 * Cuerpo JSON uniforme para errores de la API de reportes (PA3).
 *
 * @param status código HTTP
 * @param error descripción corta del estado HTTP
 * @param message mensaje útil para el consumidor
 * @param parametro parámetro que originó el error (puede ser {@code null})
 * @param path ruta solicitada
 * @param timestamp momento en que se produjo el error
 */
public record ErrorResponse(
    int status,
    String error,
    String message,
    String parametro,
    String path,
    LocalDateTime timestamp
) {
}
