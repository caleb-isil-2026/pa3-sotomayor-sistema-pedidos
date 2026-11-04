package com.isil.edu.pe.spedidos.service.impl;

import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.exception.ParametroReporteInvalidoException;
import com.isil.edu.pe.spedidos.repository.PedidoReporteJdbcRepository;
import com.isil.edu.pe.spedidos.repository.projection.ResumenPedidosProjection;
import com.isil.edu.pe.spedidos.service.PedidoReporteService;
import com.isil.edu.pe.spedidos.service.domain.ClienteFrecuente;
import com.isil.edu.pe.spedidos.service.domain.ProductoVendido;
import com.isil.edu.pe.spedidos.service.domain.ResumenPedidos;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoReporteServiceImpl implements PedidoReporteService {

  private final PedidoReporteJdbcRepository jdbcRepository;

  public PedidoReporteServiceImpl(PedidoReporteJdbcRepository jdbcRepository) {
      this.jdbcRepository = jdbcRepository;
  }

  @Override
  public ResumenPedidos obtenerResumen() {
    ResumenPedidosProjection resumenPedidos = jdbcRepository.obtenerResumen();
    return new ResumenPedidos(resumenPedidos.totalPedidos(), resumenPedidos.totalUnidades(),
        resumenPedidos.importeAcumulado(), resumenPedidos.ticketPromedio());
  }

  @Override
  public List<Pedido> buscarPorCliente(String cliente) {
    if (cliente == null || cliente.isBlank()) {
      return List.of();
    }

    return jdbcRepository.buscarPorCliente(cliente);
  }

  @Override
  public List<ProductoVendido> listarProductosMasVendidos() {
    return jdbcRepository.listarProductosMasVendidos()
        .stream()
        .map(item ->
            new ProductoVendido(
                item.productoCodigo(),
                item.productoNombre(),
                item.totalPedidos(),
                item.totalUnidades(),
                item.importeAcumulado()
            )
        )
        .toList();

  }

  /**
   * PA3: valida los filtros antes de delegar al repositorio JDBC.
   *
   * <p>Las reglas viven aquí (no en el repositorio ni en los controladores),
   * por eso la vista JSP y la API JSON comparten exactamente el mismo
   * comportamiento. Si un parámetro es inválido, la consulta SQL no se ejecuta.</p>
   */
  @Override
  @Transactional(readOnly = true)
  public List<Pedido> buscarPedidosPorClienteYTotalMinimo(String cliente, BigDecimal totalMinimo) {
    if (cliente == null || cliente.isBlank()) {
      throw new ParametroReporteInvalidoException(
          "cliente", "El cliente es obligatorio y no puede estar vacío.");
    }
    if (totalMinimo == null) {
      throw new ParametroReporteInvalidoException(
          "totalMinimo", "El total mínimo es obligatorio.");
    }
    if (totalMinimo.signum() < 0) {
      throw new ParametroReporteInvalidoException(
          "totalMinimo", "El total mínimo debe ser mayor o igual a 0.");
    }

    return jdbcRepository.buscarPedidosPorClienteYTotalMinimo(cliente.trim(), totalMinimo);
  }

  /**
   * PA3: valida el umbral y transforma la proyección JDBC al modelo de dominio.
   */
  @Override
  @Transactional(readOnly = true)
  public List<ClienteFrecuente> listarClientesFrecuentes(Integer minPedidos) {
    if (minPedidos == null) {
      throw new ParametroReporteInvalidoException(
          "minPedidos", "El número mínimo de pedidos es obligatorio.");
    }
    if (minPedidos < 1) {
      throw new ParametroReporteInvalidoException(
          "minPedidos", "El número mínimo de pedidos debe ser mayor o igual a 1.");
    }

    return jdbcRepository.listarClientesFrecuentes(minPedidos)
        .stream()
        .map(item -> new ClienteFrecuente(
            item.cliente(),
            item.totalPedidos(),
            item.totalUnidades(),
            item.importeAcumulado(),
            item.ticketPromedio()
        ))
        .toList();
  }
}
