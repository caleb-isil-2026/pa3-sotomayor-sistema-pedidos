package com.isil.edu.pe.spedidos.exception;

/**
 * Excepción de negocio lanzada por la capa Service cuando un parámetro de
 * consulta de reportes no cumple las reglas definidas (PA3).
 *
 * <p>Ejemplos: cliente vacío, total mínimo negativo o un umbral de pedidos
 * menor que 1. La capa web la traduce a HTTP 400 (API JSON) o a un mensaje
 * en la vista JSP; en ningún caso se ejecuta la consulta SQL.</p>
 */
public class ParametroReporteInvalidoException extends RuntimeException {

  private final String parametro;

  public ParametroReporteInvalidoException(String parametro, String message) {
    super(message);
    this.parametro = parametro;
  }

  public String getParametro() {
    return parametro;
  }
}
