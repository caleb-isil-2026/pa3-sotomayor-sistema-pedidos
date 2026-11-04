package com.isil.edu.pe.spedidos.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.isil.edu.pe.spedidos.repository.PedidoReporteJdbcRepository;
import com.isil.edu.pe.spedidos.repository.projection.ClienteFrecuenteProjection;
import com.isil.edu.pe.spedidos.service.impl.PedidoReporteServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * PA3 - Pruebas del {@link PedidoReporteController} (API JSON) con MockMvc
 * standalone. Se usa el Service real y se aísla el repositorio JDBC con
 * Mockito, para comprobar el recorrido Controller -> Service y que los
 * parámetros inválidos responden HTTP 400 sin consultar la base de datos.
 */
class PedidoReporteControllerPa3Test {

  private PedidoReporteJdbcRepository jdbcRepository;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    jdbcRepository = mock(PedidoReporteJdbcRepository.class);
    PedidoReporteController controller =
        new PedidoReporteController(new PedidoReporteServiceImpl(jdbcRepository));
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new ReporteApiExceptionHandler())
        .build();
  }

  @Test
  @DisplayName("Return HTTP 400 from advanced search endpoint when totalMinimo is negative")
  void returnBadRequestFromAdvancedSearchEndpointWhenTotalMinimoIsNegative() throws Exception {
    // Arrange
    String totalMinimo = "-10";

    // Act + Assert
    mockMvc.perform(get("/api/v1/pedidos/busqueda-avanzada")
            .param("cliente", "Ana")
            .param("totalMinimo", totalMinimo))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.parametro").value("totalMinimo"));
    verifyNoInteractions(jdbcRepository);
  }

  @Test
  @DisplayName("Return HTTP 400 from frequent customers endpoint when minPedidos is zero")
  void returnBadRequestFromFrequentCustomersEndpointWhenMinPedidosIsZero() throws Exception {
    // Act + Assert
    mockMvc.perform(get("/api/v1/pedidos/clientes-frecuentes").param("minPedidos", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.parametro").value("minPedidos"))
        .andExpect(jsonPath("$.message")
            .value("El número mínimo de pedidos debe ser mayor o igual a 1."));
    verifyNoInteractions(jdbcRepository);
  }

  @Test
  @DisplayName("Return HTTP 400 from advanced search endpoint when totalMinimo is not numeric")
  void returnBadRequestFromAdvancedSearchEndpointWhenTotalMinimoIsNotNumeric() throws Exception {
    // Act + Assert
    mockMvc.perform(get("/api/v1/pedidos/busqueda-avanzada")
            .param("cliente", "Ana")
            .param("totalMinimo", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.parametro").value("totalMinimo"));
    verifyNoInteractions(jdbcRepository);
  }

  @Test
  @DisplayName("Return frequent customers JSON from endpoint when minPedidos is valid")
  void returnFrequentCustomersJsonFromEndpointWhenMinPedidosIsValid() throws Exception {
    // Arrange
    when(jdbcRepository.listarClientesFrecuentes(2)).thenReturn(List.of(
        new ClienteFrecuenteProjection("Ana Torres", 3, 9,
            new BigDecimal("5850.00"), new BigDecimal("1950.00"))));

    // Act + Assert
    mockMvc.perform(get("/api/v1/pedidos/clientes-frecuentes").param("minPedidos", "2"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].cliente").value("Ana Torres"))
        .andExpect(jsonPath("$[0].totalPedidos").value(3))
        .andExpect(jsonPath("$[0].totalUnidades").value(9))
        .andExpect(jsonPath("$[0].importeAcumulado").value(5850.00))
        .andExpect(jsonPath("$[0].ticketPromedio").value(1950.00));
  }
}
