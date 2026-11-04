package com.isil.edu.pe.spedidos.service.domain;

import java.math.BigDecimal;

/**
 * Modelo de dominio del reporte de clientes frecuentes (PA3).
 *
 * <p>Desacopla a los controladores de la proyección técnica del repositorio
 * JDBC, igual que {@link ProductoVendido} y {@link ResumenPedidos}.</p>
 */
public record ClienteFrecuente(
    String cliente,
    long totalPedidos,
    long totalUnidades,
    BigDecimal importeAcumulado,
    BigDecimal ticketPromedio
) {
}
