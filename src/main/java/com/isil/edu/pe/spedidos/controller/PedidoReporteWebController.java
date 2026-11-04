package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.exception.ParametroReporteInvalidoException;
import com.isil.edu.pe.spedidos.service.PedidoReporteService;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controlador MVC del módulo de consulta y supervisión de pedidos (PA3).
 *
 * <p>Atiende {@code GET /reportes/pedidos} y prepara el {@link Model} con:</p>
 * <ul>
 *   <li>el resumen consolidado existente,</li>
 *   <li>el resultado de la búsqueda avanzada (cliente + total mínimo) cuando se solicita,</li>
 *   <li>los clientes frecuentes según el umbral {@code minPedidos} (por defecto 2).</li>
 * </ul>
 *
 * <p>El nombre lógico {@code reportes-pedidos} es resuelto por el ViewResolver
 * configurado ({@code spring.mvc.view.prefix/suffix}) como
 * {@code /WEB-INF/views/reportes-pedidos.jsp}. El controlador no construye HTML
 * ni contiene SQL ni reglas de negocio: solo convierte texto a número y delega
 * en {@link PedidoReporteService}, el mismo Service que usa la API JSON.</p>
 */
@Controller
@RequestMapping("/reportes")
public class PedidoReporteWebController {

  static final String VISTA_REPORTES = "reportes-pedidos";

  private static final String MIN_PEDIDOS_POR_DEFECTO = "2";

  private static final Logger log = LoggerFactory.getLogger(PedidoReporteWebController.class);

  private final PedidoReporteService reporteService;

  public PedidoReporteWebController(PedidoReporteService reporteService) {
    this.reporteService = reporteService;
  }

  @GetMapping("/pedidos")
  public String reportePedidos(
      @RequestParam(required = false) String cliente,
      @RequestParam(required = false) String totalMinimo,
      @RequestParam(required = false) String minPedidos,
      Model model,
      HttpServletResponse response) {

    try {
      model.addAttribute("resumen", reporteService.obtenerResumen());

      cargarBusquedaAvanzada(cliente, totalMinimo, model, response);

      cargarClientesFrecuentes(minPedidos, model, response);

      return VISTA_REPORTES;
    } catch (DataAccessException e) {
      log.error("No fue posible consultar la base de datos de pedidos.", e);
      response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
      model.addAttribute("error",
          "No fue posible consultar la base de datos de pedidos. Intente nuevamente.");
      return "error";
    }
  }

  private void cargarBusquedaAvanzada(String cliente, String totalMinimo, Model model,
      HttpServletResponse response) {

    boolean busquedaSolicitada = cliente != null || totalMinimo != null;
    model.addAttribute("busquedaRealizada", busquedaSolicitada);
    model.addAttribute("clienteBuscado", cliente);
    model.addAttribute("totalMinimoBuscado", totalMinimo);

    if (!busquedaSolicitada) {
      return;
    }

    try {
      model.addAttribute("pedidosFiltrados",
          reporteService.buscarPedidosPorClienteYTotalMinimo(
              cliente, convertirDecimal("totalMinimo", totalMinimo)));
    } catch (ParametroReporteInvalidoException e) {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      model.addAttribute("errorBusqueda", e.getMessage());
    }
  }

  private void cargarClientesFrecuentes(String minPedidos, Model model,
      HttpServletResponse response) {

    String umbral = minPedidos == null ? MIN_PEDIDOS_POR_DEFECTO : minPedidos;
    model.addAttribute("minPedidos", umbral);

    try {
      model.addAttribute("clientesFrecuentes",
          reporteService.listarClientesFrecuentes(convertirEntero("minPedidos", umbral)));
    } catch (ParametroReporteInvalidoException e) {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      model.addAttribute("errorFrecuentes", e.getMessage());
    }
  }

  /**
   * Conversión de texto a número (responsabilidad web). Un texto vacío se
   * entrega como {@code null} para que el Service aplique su regla de
   * obligatoriedad.
   */
  private BigDecimal convertirDecimal(String parametro, String valor) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(valor.trim());
    } catch (NumberFormatException e) {
      throw new ParametroReporteInvalidoException(parametro,
          "El total mínimo debe ser un número válido.");
    }
  }

  private Integer convertirEntero(String parametro, String valor) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    try {
      return Integer.valueOf(valor.trim());
    } catch (NumberFormatException e) {
      throw new ParametroReporteInvalidoException(parametro,
          "El número mínimo de pedidos debe ser un número entero.");
    }
  }
}
