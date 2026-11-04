package com.isil.edu.pe.spedidos.repository;

import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.repository.mapper.PedidoRowMapper;
import com.isil.edu.pe.spedidos.repository.projection.ClienteFrecuenteProjection;
import com.isil.edu.pe.spedidos.repository.projection.ProductoVendidoProjection;
import com.isil.edu.pe.spedidos.repository.projection.ResumenPedidosProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de consultas y reportes implementado
 * mediante Spring JDBC.
 */
@Repository
public class PedidoReporteJdbcRepository {

  private static final String COLUMNAS_PEDIDO = """
      id,
      cliente,
      producto_codigo,
      producto_nombre,
      precio_unitario,
      cantidad,
      total,
      fecha
      """;

  private final JdbcTemplate jdbcTemplate;

  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  private final PedidoRowMapper pedidoRowMapper;

  public PedidoReporteJdbcRepository(JdbcTemplate jdbcTemplate,
      NamedParameterJdbcTemplate namedParameterJdbcTemplate,
      PedidoRowMapper pedidoRowMapper) {

    this.jdbcTemplate = jdbcTemplate;
    this.namedParameterJdbcTemplate =
        namedParameterJdbcTemplate;
    this.pedidoRowMapper = pedidoRowMapper;
  }

  /**
   * Obtiene indicadores consolidados de todos
   * los pedidos registrados.
   */
  public ResumenPedidosProjection obtenerResumen() {
    String sql = """
        SELECT
            COUNT(*) AS total_pedidos,
            COALESCE(SUM(cantidad), 0)
                AS total_unidades,
            COALESCE(SUM(total), 0)
                AS importe_acumulado,
            COALESCE(AVG(total), 0)
                AS ticket_promedio
        FROM pedido
        """;

    return jdbcTemplate.queryForObject(
        sql,
        (rs, rowNum) ->
            new ResumenPedidosProjection(
                rs.getLong("total_pedidos"),
                rs.getLong("total_unidades"),
                valorMonetario(
                    rs.getBigDecimal(
                        "importe_acumulado"
                    )
                ),
                valorMonetario(
                    rs.getBigDecimal(
                        "ticket_promedio"
                    )
                )
            )
    );
  }

  /**
   * Busca pedidos utilizando un parámetro nombrado.
   */
  public List<Pedido> buscarPorCliente(String cliente) {
    String sql = """
        SELECT %s
        FROM pedido
        WHERE LOWER(cliente)
              LIKE LOWER(:cliente)
        ORDER BY id DESC
        """.formatted(COLUMNAS_PEDIDO);

    Map<String, Object> parametros = Map.of(
        "cliente",
        "%" + cliente.trim() + "%"
    );

    return namedParameterJdbcTemplate.query(
        sql,
        parametros,
        pedidoRowMapper
    );
  }

  /**
   * Consolida los pedidos por producto.
   */
  public List<ProductoVendidoProjection> listarProductosMasVendidos() {
    String sql = """
        SELECT
            producto_codigo,
            producto_nombre,
            COUNT(*) AS total_pedidos,
            SUM(cantidad) AS total_unidades,
            SUM(total) AS importe_acumulado
        FROM pedido
        GROUP BY
            producto_codigo,
            producto_nombre
        ORDER BY
            total_unidades DESC,
            importe_acumulado DESC
        """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) ->
            new ProductoVendidoProjection(
                rs.getString(
                    "producto_codigo"
                ),
                rs.getString(
                    "producto_nombre"
                ),
                rs.getLong(
                    "total_pedidos"
                ),
                rs.getLong(
                    "total_unidades"
                ),
                valorMonetario(
                    rs.getBigDecimal(
                        "importe_acumulado"
                    )
                )
            )
    );
  }

  /**
   * PA3 - Consulta 1: búsqueda avanzada por cliente y total mínimo.
   *
   * <p>Usa {@link NamedParameterJdbcTemplate} con parámetros con nombre
   * ({@code :cliente} y {@code :totalMinimo}); los valores ingresados por el
   * usuario nunca se concatenan en el SQL. Los comodines {@code %} y {@code _}
   * escritos por el usuario se escapan para que se busquen como texto literal.
   * El resultado es un {@link Pedido}, por lo que se reutiliza
   * {@link PedidoRowMapper}.</p>
   *
   * @param cliente texto a buscar dentro del nombre del cliente
   * @param totalMinimo importe total mínimo (inclusive) del pedido
   * @return pedidos que cumplen ambos filtros, ordenados por id descendente
   */
  public List<Pedido> buscarPedidosPorClienteYTotalMinimo(
      String cliente, BigDecimal totalMinimo) {

    String sql = """
        SELECT %s
        FROM pedido
        WHERE LOWER(cliente) LIKE LOWER(:cliente) ESCAPE '!'
          AND total >= :totalMinimo
        ORDER BY id DESC
        """.formatted(COLUMNAS_PEDIDO);

    MapSqlParameterSource parametros = new MapSqlParameterSource()
        .addValue("cliente", "%" + escaparLike(cliente.trim()) + "%")
        .addValue("totalMinimo", totalMinimo);

    return namedParameterJdbcTemplate.query(sql, parametros, pedidoRowMapper);
  }

  /**
   * PA3 - Consulta 2: clientes frecuentes.
   *
   * <p>Agrupa los pedidos por cliente ({@code GROUP BY}) y conserva únicamente
   * los grupos con al menos {@code :minPedidos} pedidos ({@code HAVING}). El
   * resultado agregado se transforma en {@link ClienteFrecuenteProjection}.</p>
   *
   * @param minPedidos cantidad mínima de pedidos para considerar frecuente a un cliente
   * @return clientes frecuentes ordenados por número de pedidos e importe acumulado
   */
  public List<ClienteFrecuenteProjection> listarClientesFrecuentes(int minPedidos) {

    String sql = """
        SELECT
            cliente,
            COUNT(*) AS total_pedidos,
            COALESCE(SUM(cantidad), 0) AS total_unidades,
            COALESCE(SUM(total), 0) AS importe_acumulado,
            COALESCE(AVG(total), 0) AS ticket_promedio
        FROM pedido
        GROUP BY cliente
        HAVING COUNT(*) >= :minPedidos
        ORDER BY total_pedidos DESC, importe_acumulado DESC, cliente ASC
        """;

    MapSqlParameterSource parametros = new MapSqlParameterSource()
        .addValue("minPedidos", minPedidos);

    return namedParameterJdbcTemplate.query(
        sql,
        parametros,
        (rs, rowNum) -> new ClienteFrecuenteProjection(
            rs.getString("cliente"),
            rs.getLong("total_pedidos"),
            rs.getLong("total_unidades"),
            valorMonetario(rs.getBigDecimal("importe_acumulado")),
            valorMonetario(rs.getBigDecimal("ticket_promedio"))
                .setScale(2, RoundingMode.HALF_UP)
        )
    );
  }

  /**
   * Escapa los comodines de LIKE para que el texto del usuario se compare
   * literalmente. Se usa '!' como carácter de escape (declarado en el SQL).
   */
  private String escaparLike(String texto) {
    return texto
        .replace("!", "!!")
        .replace("%", "!%")
        .replace("_", "!_");
  }

  private BigDecimal valorMonetario(
      BigDecimal valor) {

    return valor != null
        ? valor
        : BigDecimal.ZERO;
  }
}
