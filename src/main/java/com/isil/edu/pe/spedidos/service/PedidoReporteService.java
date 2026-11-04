package com.isil.edu.pe.spedidos.service;

import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.exception.ParametroReporteInvalidoException;
import com.isil.edu.pe.spedidos.service.domain.ClienteFrecuente;
import com.isil.edu.pe.spedidos.service.domain.ProductoVendido;
import com.isil.edu.pe.spedidos.service.domain.ResumenPedidos;
import java.math.BigDecimal;
import java.util.List;

public interface PedidoReporteService {

  ResumenPedidos obtenerResumen();

  List<Pedido> buscarPorCliente(String cliente);

  List<ProductoVendido> listarProductosMasVendidos();

  /**
   * PA3 - Búsqueda avanzada de pedidos por cliente y total mínimo.
   *
   * @param cliente texto del cliente; obligatorio y no vacío
   * @param totalMinimo importe mínimo; obligatorio y mayor o igual a 0
   * @return pedidos que cumplen ambos filtros, ordenados por id descendente
   * @throws ParametroReporteInvalidoException si algún parámetro es inválido
   */
  List<Pedido> buscarPedidosPorClienteYTotalMinimo(String cliente, BigDecimal totalMinimo);

  /**
   * PA3 - Clientes con una cantidad de pedidos mayor o igual al umbral.
   *
   * @param minPedidos umbral mínimo de pedidos; obligatorio y mayor o igual a 1
   * @return clientes frecuentes con sus indicadores agregados
   * @throws ParametroReporteInvalidoException si el umbral es inválido
   */
  List<ClienteFrecuente> listarClientesFrecuentes(Integer minPedidos);

}
