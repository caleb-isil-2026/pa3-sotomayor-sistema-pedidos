package com.isil.edu.pe.spedidos.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * PA3 - Pruebas de integración MVC + Spring Security del nuevo módulo de
 * reportes ({@code GET /reportes/pedidos} y endpoints JSON nuevos), con H2.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedidoReporteWebSecurityPa3IntegrationTest {

  private static final String REPORTES_PATH = "/reportes/pedidos";

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();
    jdbcTemplate.update("DELETE FROM pedido");
    insertarPedido("Ana Torres", "5000.00", 2);
    insertarPedido("Ana Torres", "100.00", 2);
    insertarPedido("Carlos Perez", "150.00", 3);
  }

  @Test
  @DisplayName("Render reportes-pedidos JSP with summary and frequent customers when CONSULTA requests GET /reportes/pedidos")
  void renderReportsViewWithModelWhenConsultaRoleRequestsReportsPage() throws Exception {
    mockMvc.perform(get(REPORTES_PATH).with(user("consulta").roles("CONSULTA")))
        .andExpect(status().isOk())
        .andExpect(view().name("reportes-pedidos"))
        .andExpect(model().attributeExists("resumen"))
        .andExpect(model().attribute("minPedidos", "2"))
        .andExpect(model().attribute("clientesFrecuentes", hasSize(1)));
  }

  @Test
  @DisplayName("Load filtered orders into the JSP model when CONSULTA submits customer and minimum total")
  void loadFilteredOrdersIntoModelWhenConsultaSubmitsAdvancedSearch() throws Exception {
    mockMvc.perform(get(REPORTES_PATH)
            .param("cliente", "Ana")
            .param("totalMinimo", "500")
            .with(user("consulta").roles("CONSULTA")))
        .andExpect(status().isOk())
        .andExpect(view().name("reportes-pedidos"))
        .andExpect(model().attribute("pedidosFiltrados", hasSize(1)));
  }

  @Test
  @DisplayName("Return HTTP 400 and error message in JSP model when minPedidos is less than 1")
  void returnBadRequestWithErrorInViewWhenMinPedidosIsLessThanOne() throws Exception {
    mockMvc.perform(get(REPORTES_PATH)
            .param("minPedidos", "0")
            .with(user("operador").roles("OPERADOR")))
        .andExpect(status().isBadRequest())
        .andExpect(view().name("reportes-pedidos"))
        .andExpect(model().attributeExists("errorFrecuentes"))
        .andExpect(model().attributeDoesNotExist("clientesFrecuentes"));
  }

  @Test
  @DisplayName("Redirect anonymous user to login when accessing GET /reportes/pedidos")
  void redirectAnonymousUserToLoginWhenAccessingReportsPage() throws Exception {
    mockMvc.perform(get(REPORTES_PATH))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  @DisplayName("Redirect anonymous user to login when calling new frequent customers endpoint")
  void redirectAnonymousUserToLoginWhenCallingFrequentCustomersEndpoint() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/clientes-frecuentes").param("minPedidos", "2"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  @DisplayName("Forbid GET /reportes/pedidos when authenticated user has no OPERADOR or CONSULTA role")
  void forbidReportsPageWhenUserHasNoReportRole() throws Exception {
    mockMvc.perform(get(REPORTES_PATH).with(user("invitado").roles("INVITADO")))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Return filtered JSON from advanced search endpoint when CONSULTA sends valid filters")
  void returnFilteredJsonFromAdvancedSearchWhenConsultaSendsValidFilters() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/busqueda-avanzada")
            .param("cliente", "ana")
            .param("totalMinimo", "100")
            .with(user("consulta").roles("CONSULTA")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].cliente").value("Ana Torres"));
  }

  private void insertarPedido(String cliente, String total, int cantidad) {
    jdbcTemplate.update(
        """
        INSERT INTO pedido (
          cliente, producto_codigo, producto_nombre,
          precio_unitario, cantidad, total, fecha
        ) VALUES (?, 'LAP-001', 'Laptop', ?, ?, ?, CURRENT_TIMESTAMP)
        """,
        cliente, new BigDecimal(total), cantidad, new BigDecimal(total));
  }
}
