package com.isil.edu.pe.spedidos.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.exception.ParametroReporteInvalidoException;
import com.isil.edu.pe.spedidos.repository.PedidoReporteJdbcRepository;
import com.isil.edu.pe.spedidos.repository.projection.ClienteFrecuenteProjection;
import com.isil.edu.pe.spedidos.service.domain.ClienteFrecuente;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PA3 - Pruebas unitarias (JUnit 5 + Mockito) de las validaciones de
 * {@link PedidoReporteServiceImpl}. El repositorio JDBC se aísla con un mock
 * para comprobar que un parámetro inválido nunca llega a ejecutar SQL.
 */
class PedidoReporteServiceImplPa3Test {

  private PedidoReporteJdbcRepository jdbcRepository;

  private PedidoReporteServiceImpl reporteService;

  @BeforeEach
  void setUp() {
    jdbcRepository = mock(PedidoReporteJdbcRepository.class);
    reporteService = new PedidoReporteServiceImpl(jdbcRepository);
  }

  @Test
  @DisplayName("Reject frequent customers report in PedidoReporteService when minPedidos is zero")
  void rejectFrequentCustomersInServiceWhenMinPedidosIsZero() {
    // Arrange
    Integer minPedidos = 0;

    // Act
    ParametroReporteInvalidoException error = assertThrows(
        ParametroReporteInvalidoException.class,
        () -> reporteService.listarClientesFrecuentes(minPedidos));

    // Assert
    assertEquals("minPedidos", error.getParametro());
    verifyNoInteractions(jdbcRepository);
  }

  @Test
  @DisplayName("Reject advanced search in PedidoReporteService when totalMinimo is negative")
  void rejectAdvancedSearchInServiceWhenTotalMinimoIsNegative() {
    // Arrange
    BigDecimal totalMinimo = new BigDecimal("-1");

    // Act
    ParametroReporteInvalidoException error = assertThrows(
        ParametroReporteInvalidoException.class,
        () -> reporteService.buscarPedidosPorClienteYTotalMinimo("Ana", totalMinimo));

    // Assert
    assertEquals("totalMinimo", error.getParametro());
    assertEquals("El total mínimo debe ser mayor o igual a 0.", error.getMessage());
    verifyNoInteractions(jdbcRepository);
  }

  @Test
  @DisplayName("Reject advanced search in PedidoReporteService when customer is blank")
  void rejectAdvancedSearchInServiceWhenCustomerIsBlank() {
    // Arrange
    String cliente = "   ";

    // Act
    ParametroReporteInvalidoException error = assertThrows(
        ParametroReporteInvalidoException.class,
        () -> reporteService.buscarPedidosPorClienteYTotalMinimo(cliente, BigDecimal.TEN));

    // Assert
    assertEquals("cliente", error.getParametro());
    verifyNoInteractions(jdbcRepository);
  }

  @Test
  @DisplayName("Delegate trimmed customer to JDBC repository from PedidoReporteService when filters are valid")
  void delegateTrimmedCustomerToRepositoryWhenAdvancedSearchFiltersAreValid() {
    // Arrange
    BigDecimal totalMinimo = new BigDecimal("500.00");
    List<Pedido> esperados = List.of(new Pedido());
    when(jdbcRepository.buscarPedidosPorClienteYTotalMinimo("Ana", totalMinimo)).thenReturn(esperados);

    // Act
    List<Pedido> pedidos = reporteService.buscarPedidosPorClienteYTotalMinimo("  Ana  ", totalMinimo);

    // Assert
    assertEquals(1, pedidos.size());
    verify(jdbcRepository).buscarPedidosPorClienteYTotalMinimo("Ana", totalMinimo);
  }

  @Test
  @DisplayName("Map JDBC projection to domain model in PedidoReporteService when minPedidos is valid")
  void mapProjectionToDomainInServiceWhenMinPedidosIsValid() {
    // Arrange
    when(jdbcRepository.listarClientesFrecuentes(2)).thenReturn(List.of(
        new ClienteFrecuenteProjection("Ana Torres", 3, 9,
            new BigDecimal("5850.00"), new BigDecimal("1950.00"))));

    // Act
    List<ClienteFrecuente> clientes = reporteService.listarClientesFrecuentes(2);

    // Assert
    assertEquals(1, clientes.size());
    assertEquals("Ana Torres", clientes.get(0).cliente());
    assertEquals(3L, clientes.get(0).totalPedidos());
    assertEquals(new BigDecimal("1950.00"), clientes.get(0).ticketPromedio());
  }
}
