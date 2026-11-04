package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.controller.dto.ErrorResponse;
import com.isil.edu.pe.spedidos.exception.ParametroReporteInvalidoException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce los errores de la API de reportes a respuestas JSON coherentes (PA3).
 *
 * <p>Solo aplica a {@link PedidoReporteController}, de modo que no altera el
 * comportamiento de las demás APIs ni de los controladores JSP.</p>
 *
 * <ul>
 *   <li>Parámetro inválido según las reglas del Service: HTTP 400.</li>
 *   <li>Parámetro ausente o con formato incorrecto: HTTP 400.</li>
 *   <li>{@link DataAccessException}: HTTP 503 con un mensaje útil
 *       (la excepción se traduce, no solo se imprime).</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = PedidoReporteController.class)
public class ReporteApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ReporteApiExceptionHandler.class);

  @ExceptionHandler(ParametroReporteInvalidoException.class)
  public ResponseEntity<ErrorResponse> parametroInvalido(
      ParametroReporteInvalidoException e, HttpServletRequest request) {
    return respuesta(HttpStatus.BAD_REQUEST, e.getMessage(), e.getParametro(), request);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> parametroAusente(
      MissingServletRequestParameterException e, HttpServletRequest request) {
    return respuesta(HttpStatus.BAD_REQUEST,
        "El parámetro " + e.getParameterName() + " es obligatorio.",
        e.getParameterName(), request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> formatoInvalido(
      MethodArgumentTypeMismatchException e, HttpServletRequest request) {
    return respuesta(HttpStatus.BAD_REQUEST,
        "El parámetro " + e.getName() + " debe ser numérico.",
        e.getName(), request);
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ErrorResponse> errorBaseDatos(
      DataAccessException e, HttpServletRequest request) {
    log.error("Error de acceso a datos al consultar reportes.", e);
    return respuesta(HttpStatus.SERVICE_UNAVAILABLE,
        "No fue posible consultar la base de datos de pedidos. Intente nuevamente.",
        null, request);
  }

  private ResponseEntity<ErrorResponse> respuesta(
      HttpStatus status, String mensaje, String parametro, HttpServletRequest request) {
    ErrorResponse body = new ErrorResponse(
        status.value(),
        status.getReasonPhrase(),
        mensaje,
        parametro,
        request.getRequestURI(),
        LocalDateTime.now()
    );
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }
}
