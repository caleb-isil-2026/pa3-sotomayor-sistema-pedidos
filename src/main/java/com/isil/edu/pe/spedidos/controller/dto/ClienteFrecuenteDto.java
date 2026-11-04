package com.isil.edu.pe.spedidos.controller.dto;

import com.isil.edu.pe.spedidos.service.domain.ClienteFrecuente;
import java.math.BigDecimal;

/**
 * Contrato JSON del reporte de clientes frecuentes (PA3).
 */
public record ClienteFrecuenteDto(
    String cliente,
    long totalPedidos,
    long totalUnidades,
    BigDecimal importeAcumulado,
    BigDecimal ticketPromedio
) {

  public static ClienteFrecuenteDto from(ClienteFrecuente cliente) {
    return new ClienteFrecuenteDto(
        cliente.cliente(),
        cliente.totalPedidos(),
        cliente.totalUnidades(),
        cliente.importeAcumulado(),
        cliente.ticketPromedio()
    );
  }
}
