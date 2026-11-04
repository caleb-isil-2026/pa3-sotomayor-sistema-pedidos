package com.isil.edu.pe.spedidos.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.repository.projection.ClienteFrecuenteProjection;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * PA3 - Pruebas de integración (H2, perfil test) de las dos consultas nuevas
 * implementadas con Spring JDBC en {@link PedidoReporteJdbcRepository}.
 *
 * <p>Datos de prueba comunes:</p>
 * <pre>
 * Ana Torres   LAP-001 x2  total 5000.00
 * Ana Torres   MOU-001 x2  total  100.00
 * Carlos Perez MOU-001 x3  total  150.00
 * Ana Torres   TEC-001 x5  total  750.00
 * Carlos Perez LAP-001 x1  total 2500.00
 * Diana Rojas  MOU-001 x1  total   50.00
 * </pre>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedidoReporteJdbcRepositoryPa3Test {

  @Autowired
  private PedidoReporteJdbcRepository repository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM pedido");
    insertarPedido("Ana Torres", "LAP-001", "Laptop", "2500.00", 2, "5000.00");
    insertarPedido("Ana Torres", "MOU-001", "Mouse", "50.00", 2, "100.00");
    insertarPedido("Carlos Perez", "MOU-001", "Mouse", "50.00", 3, "150.00");
    insertarPedido("Ana Torres", "TEC-001", "Teclado", "150.00", 5, "750.00");
    insertarPedido("Carlos Perez", "LAP-001", "Laptop", "2500.00", 1, "2500.00");
    insertarPedido("Diana Rojas", "MOU-001", "Mouse", "50.00", 1, "50.00");
  }

  @Test
  @DisplayName("Return only orders matching customer and minimum total from JDBC repository, ordered by id desc")
  void returnOnlyMatchingOrdersFromJdbcRepositoryWhenCustomerAndMinimumTotalAreProvided() {
    // Arrange
    String cliente = "ana";
    BigDecimal totalMinimo = new BigDecimal("500.00");

    // Act
    List<Pedido> pedidos = repository.buscarPedidosPorClienteYTotalMinimo(cliente, totalMinimo);

    // Assert
    assertEquals(2, pedidos.size());
    assertTrue(pedidos.stream().allMatch(p -> "Ana Torres".equals(p.getCliente())));
    assertTrue(pedidos.stream().allMatch(p -> p.getTotal().compareTo(totalMinimo) >= 0));
    assertEquals(0, new BigDecimal("750.00").compareTo(pedidos.get(0).getTotal()));
    assertEquals(0, new BigDecimal("5000.00").compareTo(pedidos.get(1).getTotal()));
    assertTrue(pedidos.get(0).getId() > pedidos.get(1).getId(), "Debe ordenar por id descendente");
  }

  @Test
  @DisplayName("Treat SQL fragments and LIKE wildcards as literal text in JDBC advanced search")
  void returnNoOrdersFromJdbcRepositoryWhenCustomerContainsSqlOrWildcards() {
    // Arrange
    String inyeccion = "' OR '1'='1";
    String comodin = "%";

    // Act
    List<Pedido> porInyeccion = repository.buscarPedidosPorClienteYTotalMinimo(inyeccion, BigDecimal.ZERO);
    List<Pedido> porComodin = repository.buscarPedidosPorClienteYTotalMinimo(comodin, BigDecimal.ZERO);

    // Assert
    assertTrue(porInyeccion.isEmpty());
    assertTrue(porComodin.isEmpty());
    assertEquals(6, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pedido", Integer.class));
  }

  @Test
  @DisplayName("Return aggregated values with GROUP BY/HAVING from JDBC repository when minPedidos is 2")
  void returnFrequentCustomersWithAggregatesFromJdbcRepositoryWhenMinPedidosIsTwo() {
    // Arrange
    int minPedidos = 2;

    // Act
    List<ClienteFrecuenteProjection> clientes = repository.listarClientesFrecuentes(minPedidos);

    // Assert
    assertEquals(2, clientes.size(), "Diana Rojas (1 pedido) no debe aparecer");

    ClienteFrecuenteProjection ana = clientes.get(0);
    assertEquals("Ana Torres", ana.cliente());
    assertEquals(3L, ana.totalPedidos());
    assertEquals(9L, ana.totalUnidades());
    assertEquals(0, new BigDecimal("5850.00").compareTo(ana.importeAcumulado()));
    assertEquals(new BigDecimal("1950.00"), ana.ticketPromedio());

    ClienteFrecuenteProjection carlos = clientes.get(1);
    assertEquals("Carlos Perez", carlos.cliente());
    assertEquals(2L, carlos.totalPedidos());
    assertEquals(4L, carlos.totalUnidades());
    assertEquals(0, new BigDecimal("2650.00").compareTo(carlos.importeAcumulado()));
    assertEquals(new BigDecimal("1325.00"), carlos.ticketPromedio());
  }

  @Test
  @DisplayName("Exclude customers below threshold from JDBC frequent customers when minPedidos is 3")
  void returnOnlyCustomerAboveThresholdFromJdbcRepositoryWhenMinPedidosIsThree() {
    // Arrange
    int minPedidos = 3;

    // Act
    List<ClienteFrecuenteProjection> clientes = repository.listarClientesFrecuentes(minPedidos);

    // Assert
    assertEquals(1, clientes.size());
    assertEquals("Ana Torres", clientes.get(0).cliente());
  }

  private void insertarPedido(String cliente, String productoCodigo, String productoNombre,
      String precioUnitario, int cantidad, String total) {
    jdbcTemplate.update(
        """
        INSERT INTO pedido (
          cliente, producto_codigo, producto_nombre,
          precio_unitario, cantidad, total, fecha
        ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """,
        cliente, productoCodigo, productoNombre,
        new BigDecimal(precioUnitario), cantidad, new BigDecimal(total));
  }
}
