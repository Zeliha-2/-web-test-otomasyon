package com.automation.db;

import com.automation.config.ConfigManager;
import com.automation.models.DbScenario;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class H2DbClient {
    private static final String PRODUCTS_TABLE = "qa_products";

    private H2DbClient() {
    }

    public static Connection openConnection() throws SQLException {
        loadDriver();
        return DriverManager.getConnection(
                ConfigManager.get("db.h2.url"),
                ConfigManager.get("db.h2.user"),
                ConfigManager.get("db.h2.password") == null ? "" : ConfigManager.get("db.h2.password"));
    }

    public static boolean ping() {
        try (Connection conn = openConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            System.out.println("[DB-WARN] H2 ping failed: " + e.getMessage());
            return false;
        }
    }

    public static void createProductsTable(Connection conn) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS qa_products (
                    id INT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    category VARCHAR(50),
                    price DECIMAL(10, 2) NOT NULL,
                    quantity INT NOT NULL DEFAULT 0
                )
                """;
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    public static int insertProduct(Connection conn, DbScenario scenario) throws SQLException {
        String sql = "INSERT INTO qa_products (id, name, category, price, quantity) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scenario.getRecordId());
            ps.setString(2, scenario.getName());
            ps.setString(3, scenario.getCategory());
            ps.setDouble(4, scenario.getPrice());
            ps.setInt(5, scenario.getQuantity() == null ? 0 : scenario.getQuantity());
            return ps.executeUpdate();
        }
    }

    public static int countProducts(Connection conn) throws SQLException {
        return countProducts(conn, null);
    }

    public static int countProducts(Connection conn, Integer recordId) throws SQLException {
        String sql = recordId == null
                ? "SELECT COUNT(1) FROM qa_products"
                : "SELECT COUNT(1) FROM qa_products WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (recordId != null) {
                ps.setInt(1, recordId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public static String readColumn(Connection conn, int recordId, String column) throws SQLException {
        String sql = "SELECT " + sanitizeColumn(column) + " FROM qa_products WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getString(1);
            }
        }
    }

    public static int sumQuantity(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM qa_products";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static int updateProduct(Connection conn, DbScenario scenario) throws SQLException {
        String sql = """
                UPDATE qa_products
                SET name = COALESCE(?, name),
                    category = COALESCE(?, category),
                    price = COALESCE(?, price),
                    quantity = COALESCE(?, quantity)
                WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, scenario.getName());
            ps.setString(2, scenario.getCategory());
            if (scenario.getPrice() == null) {
                ps.setNull(3, java.sql.Types.DECIMAL);
            } else {
                ps.setDouble(3, scenario.getPrice());
            }
            if (scenario.getQuantity() == null) {
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setInt(4, scenario.getQuantity());
            }
            ps.setInt(5, scenario.getRecordId());
            return ps.executeUpdate();
        }
    }

    public static int deleteProduct(Connection conn, int recordId) throws SQLException {
        String sql = "DELETE FROM qa_products WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recordId);
            return ps.executeUpdate();
        }
    }

    private static void loadDriver() throws SQLException {
        String driver = ConfigManager.get("db.h2.driver");
        if (driver == null || driver.isBlank()) {
            return;
        }
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver not found: " + driver, e);
        }
    }

    private static String sanitizeColumn(String column) {
        if (column == null || !column.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Invalid column name: " + column);
        }
        return column;
    }

    public static String productsTableName() {
        return PRODUCTS_TABLE;
    }
}
