<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Reportes de Pedidos - ISIL</title>
    <c:url var="cssUrl" value="/assets/css/app.css"/>
    <link rel="stylesheet" href="${cssUrl}">
</head>
<body>
<div class="contenedor">
    <header class="cabecera">
        <div>
            <p class="eyebrow">PA3 · Spring MVC · Spring Security · Spring JDBC</p>
            <h1>Consulta y supervisión de pedidos</h1>
            <p class="subtitulo">
                Usuario: <strong><c:out value="${pageContext.request.userPrincipal.name}"/></strong>.
                Los reportes consultan la base local con Spring JDBC; no dependen de inventario-service.
            </p>
        </div>
        <div class="acciones-cabecera">
            <c:url var="pedidosUrl" value="/pedidos"/>
            <a class="boton-secundario" href="${pedidosUrl}">Ir a pedidos</a>
            <c:url var="logoutUrl" value="/logout"/>
            <form method="post" action="${logoutUrl}">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <button type="submit">Cerrar sesión</button>
            </form>
        </div>
    </header>

    <div class="flujo">
        Spring Security → DispatcherServlet → PedidoReporteWebController → PedidoReporteService → PedidoReporteJdbcRepository → MySQL
    </div>

    <%-- ===================== RESUMEN EXISTENTE ===================== --%>
    <section class="tarjeta">
        <h2>Resumen consolidado</h2>
        <div class="indicadores">
            <div class="indicador">
                <span>Total de pedidos</span>
                <strong><c:out value="${resumen.totalPedidos}"/></strong>
            </div>
            <div class="indicador">
                <span>Total de unidades</span>
                <strong><c:out value="${resumen.totalUnidades}"/></strong>
            </div>
            <div class="indicador">
                <span>Importe acumulado</span>
                <strong>S/ <fmt:formatNumber value="${resumen.importeAcumulado}" minFractionDigits="2" maxFractionDigits="2"/></strong>
            </div>
            <div class="indicador">
                <span>Ticket promedio</span>
                <strong>S/ <fmt:formatNumber value="${resumen.ticketPromedio}" minFractionDigits="2" maxFractionDigits="2"/></strong>
            </div>
        </div>
    </section>

    <c:url var="reportesUrl" value="/reportes/pedidos"/>

    <%-- ===================== CONSULTA 1: BÚSQUEDA AVANZADA ===================== --%>
    <section class="tarjeta">
        <div class="titulo-tabla">
            <h2>Búsqueda avanzada</h2>
            <p>Pedidos cuyo cliente contiene el texto indicado y cuyo total es mayor o igual al monto mínimo.</p>
        </div>

        <c:if test="${not empty errorBusqueda}">
            <div class="alerta error" id="error-busqueda"><c:out value="${errorBusqueda}"/></div>
        </c:if>

        <form method="get" action="${reportesUrl}" class="form-grid form-reporte">
            <label>
                Cliente
                <input name="cliente" maxlength="120" placeholder="Ej. Ana Torres"
                       value="${fn:escapeXml(clienteBuscado)}">
            </label>
            <label>
                Total mínimo (S/)
                <input name="totalMinimo" type="number" step="0.01" placeholder="Ej. 500.00"
                       value="${fn:escapeXml(totalMinimoBuscado)}">
            </label>
            <input type="hidden" name="minPedidos" value="${fn:escapeXml(minPedidos)}">
            <button type="submit">Buscar</button>
        </form>

        <c:if test="${busquedaRealizada and empty errorBusqueda}">
            <p class="resultado-info">
                <strong><c:out value="${fn:length(pedidosFiltrados)}"/></strong> pedido(s) encontrados para
                cliente «<c:out value="${clienteBuscado}"/>» con total ≥ S/ <c:out value="${totalMinimoBuscado}"/>.
            </p>
            <div class="tabla-scroll">
                <table id="tabla-busqueda">
                    <thead>
                    <tr>
                        <th>ID</th><th>Cliente</th><th>Código</th><th>Producto</th>
                        <th>Precio unitario</th><th>Cantidad</th><th>Total</th><th>Fecha</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="pedido" items="${pedidosFiltrados}">
                        <tr>
                            <td><c:out value="${pedido.id}"/></td>
                            <td><c:out value="${pedido.cliente}"/></td>
                            <td><c:out value="${pedido.productoCodigo}"/></td>
                            <td><c:out value="${pedido.productoNombre}"/></td>
                            <td>S/ <fmt:formatNumber value="${pedido.precioUnitario}" minFractionDigits="2" maxFractionDigits="2"/></td>
                            <td><c:out value="${pedido.cantidad}"/></td>
                            <td>S/ <fmt:formatNumber value="${pedido.total}" minFractionDigits="2" maxFractionDigits="2"/></td>
                            <td><c:out value="${pedido.fecha}"/></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty pedidosFiltrados}">
                        <tr><td colspan="8" class="sin-datos">No hay pedidos que cumplan ambos filtros.</td></tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
        </c:if>
    </section>

    <%-- ===================== CONSULTA 2: CLIENTES FRECUENTES ===================== --%>
    <section class="tarjeta">
        <div class="titulo-tabla">
            <h2>Clientes frecuentes</h2>
            <p>Clientes con una cantidad de pedidos mayor o igual al umbral (GROUP BY cliente HAVING COUNT(*) ≥ minPedidos).</p>
        </div>

        <c:if test="${not empty errorFrecuentes}">
            <div class="alerta error" id="error-frecuentes"><c:out value="${errorFrecuentes}"/></div>
        </c:if>

        <form method="get" action="${reportesUrl}" class="form-grid form-reporte">
            <label>
                Mínimo de pedidos
                <input name="minPedidos" type="number" step="1" value="${fn:escapeXml(minPedidos)}">
            </label>
            <c:if test="${busquedaRealizada}">
                <input type="hidden" name="cliente" value="${fn:escapeXml(clienteBuscado)}">
                <input type="hidden" name="totalMinimo" value="${fn:escapeXml(totalMinimoBuscado)}">
            </c:if>
            <span></span>
            <button type="submit">Consultar</button>
        </form>

        <c:if test="${empty errorFrecuentes}">
            <div class="tabla-scroll">
                <table id="tabla-frecuentes">
                    <thead>
                    <tr>
                        <th>Cliente</th><th>Total pedidos</th><th>Total unidades</th>
                        <th>Importe acumulado</th><th>Ticket promedio</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="frecuente" items="${clientesFrecuentes}">
                        <tr>
                            <td><c:out value="${frecuente.cliente}"/></td>
                            <td><c:out value="${frecuente.totalPedidos}"/></td>
                            <td><c:out value="${frecuente.totalUnidades}"/></td>
                            <td>S/ <fmt:formatNumber value="${frecuente.importeAcumulado}" minFractionDigits="2" maxFractionDigits="2"/></td>
                            <td>S/ <fmt:formatNumber value="${frecuente.ticketPromedio}" minFractionDigits="2" maxFractionDigits="2"/></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty clientesFrecuentes}">
                        <tr><td colspan="5" class="sin-datos">Ningún cliente alcanza el umbral indicado.</td></tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
        </c:if>
    </section>
</div>
</body>
</html>
