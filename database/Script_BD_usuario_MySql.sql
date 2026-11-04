-- =====================================================================
-- Script_BD_usuario_MySql.sql
-- Sistema de Pedidos - Base de datos y usuario de laboratorio (MySQL 8.x)
--
-- Ejecutar con una cuenta administradora (por ejemplo root):
--   mysql -u root -p < database/Script_BD_usuario_MySql.sql
--
-- La contraseña indicada es SOLO de laboratorio y coincide con el valor de
-- respaldo de application.properties. En cualquier otro entorno se debe
-- cambiar y entregar a la aplicación mediante DB_URL / DB_USERNAME / DB_PASSWORD.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS pedidosdb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'spedidos_app'@'localhost' IDENTIFIED BY 'userapp_2026!';

-- Permisos mínimos sobre la base del sistema (DDL incluido porque
-- spring.jpa.hibernate.ddl-auto=update crea/actualiza la tabla pedido).
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON pedidosdb.* TO 'spedidos_app'@'localhost';

FLUSH PRIVILEGES;

USE pedidosdb;

-- Estructura equivalente a la entidad JPA Pedido. Hibernate también puede
-- crearla automáticamente; se deja explícita para revisar el modelo.
CREATE TABLE IF NOT EXISTS pedido (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  cliente         VARCHAR(120)  NOT NULL,
  producto_codigo VARCHAR(30)   NOT NULL,
  producto_nombre VARCHAR(120)  NOT NULL,
  precio_unitario DECIMAL(12,2) NOT NULL,
  cantidad        INT           NOT NULL,
  total           DECIMAL(12,2) NOT NULL,
  fecha           DATETIME(6)   NOT NULL,
  PRIMARY KEY (id),
  -- Índice de apoyo para las consultas PA3 (filtro y GROUP BY por cliente).
  KEY idx_pedido_cliente (cliente)
) ENGINE = InnoDB;
