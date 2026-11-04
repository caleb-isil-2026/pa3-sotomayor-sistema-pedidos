package com.isil.edu.pe.spedidos.repository.projection;

import java.math.BigDecimal;

/**
 * Proyección del reporte de clientes frecuentes (PA3).
 *
 * <p>Representa una fila agregada producida por una consulta SQL con
 * {@code GROUP BY cliente HAVING COUNT(*) >= :minPedidos}. No es una entidad:
 * solo transporta los valores calculados por la base de datos.</p>
 *
 * @param cliente nombre del cliente agrupado
 * @param totalPedidos cantidad de pedidos registrados por el cliente
 * @param totalUnidades suma de unidades solicitadas por el cliente
 * @param importeAcumulado suma de los totales de sus pedidos
 * @param ticketPromedio importe promedio por pedido (2 decimales)
 */
public record ClienteFrecuenteProjection(
    String cliente,
    long totalPedidos,
    long totalUnidades,
    BigDecimal importeAcumulado,
    BigDecimal ticketPromedio
) {
}
