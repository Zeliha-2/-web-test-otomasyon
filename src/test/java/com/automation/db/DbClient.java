package com.automation.db;

import com.automation.config.ConfigManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;

public final class DbClient {
    private static final Set<String> ALLOWED_TABLES = Set.of(
            "orders", "test_results", "products", "users", "failed_tests");

    private DbClient() {
    }

    public static boolean isSqlServerAvailable() {
        if (!ConfigManager.getBoolean("run.db.ui.verify", false)) {
            return false;
        }
        try (Connection conn = openConnection()) {
            return conn.isValid(3);
        } catch (SQLException e) {
            System.out.println("[DB-WARN] SQL Server ping failed: " + e.getMessage());
            return false;
        }
    }

    public static Connection openConnectionForInit() throws SQLException {
        return openConnection();
    }

    public static boolean orderExists(String orderNo) {
        String sql = "SELECT COUNT(1) FROM orders WHERE order_no = ?";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("[DB-WARN] orderExists skipped: " + e.getMessage());
            return false;
        }
    }

    public static String getOrderStatus(String orderNo) {
        String sql = "SELECT status FROM orders WHERE order_no = ?";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            System.out.println("[DB-WARN] getOrderStatus skipped: " + e.getMessage());
            return null;
        }
    }

    public static void insertCheckoutOrder(
            String orderNo,
            String caseId,
            String productName,
            String customerEmail,
            String uiUrl,
            String status) throws SQLException {
        String sql = """
                INSERT INTO orders (order_no, case_id, product_name, customer_email, status, ui_url)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderNo);
            ps.setString(2, caseId);
            ps.setString(3, productName);
            ps.setString(4, customerEmail);
            ps.setString(5, status);
            ps.setString(6, uiUrl);
            ps.executeUpdate();
        }
    }

    public static void insertTestResult(
            String testName,
            String module,
            String status,
            String duration,
            String errorMessage) {
        String sql = """
                INSERT INTO test_results (test_name, module, status, duration, run_date, error_message)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, truncate(testName, 200));
            ps.setString(2, truncate(module, 100));
            ps.setString(3, truncate(status, 20));
            ps.setString(4, truncate(duration, 20));
            ps.setTimestamp(5, Timestamp.from(Instant.now()));
            ps.setString(6, errorMessage);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[DB-WARN] insertTestResult skipped: " + e.getMessage());
        }
    }

    public static void insertFailedTest(
            String caseId,
            String module,
            String errorMessage,
            String aiAnalysis) {
        String sql = """
                INSERT INTO failed_tests (case_id, module, error_message, ai_analysis, run_date)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, truncate(caseId, 50));
            ps.setString(2, truncate(module, 100));
            ps.setString(3, errorMessage);
            ps.setString(4, aiAnalysis);
            ps.setTimestamp(5, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[DB-WARN] insertFailedTest skipped: " + e.getMessage());
        }
    }

    public static int countTable(String tableName) throws SQLException {
        validateTable(tableName);
        String sql = "SELECT COUNT(1) FROM " + tableName;
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static int countTableById(String tableName, int id) throws SQLException {
        validateTable(tableName);
        String sql = "SELECT COUNT(1) FROM " + tableName + " WHERE id = ?";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public static String readColumnById(String tableName, int id, String column) throws SQLException {
        validateTable(tableName);
        validateColumn(column);
        String sql = "SELECT " + column + " FROM " + tableName + " WHERE id = ?";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getString(1);
            }
        }
    }

    public static int countOrdersByCaseId(String caseId) {
        String sql = "SELECT COUNT(1) FROM orders WHERE case_id = ?";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.out.println("[DB-WARN] countOrdersByCaseId skipped: " + e.getMessage());
            return 0;
        }
    }

    public static int countAllTestResults() {
        return countSingle("SELECT COUNT(1) FROM test_results");
    }

    public static int countAllFailedTests() {
        return countSingle("SELECT COUNT(1) FROM failed_tests");
    }

    public static int countAllOrders() {
        return countSingle("SELECT COUNT(1) FROM orders");
    }

    public static java.util.List<com.automation.dashboard.DbModuleStatEntry> fetchModuleStatsFromTestResults() {
        java.util.Map<String, com.automation.dashboard.DbModuleStatEntry> byModule = new java.util.LinkedHashMap<>();
        String sql = """
                SELECT module, status, COUNT(1) AS cnt
                FROM test_results
                GROUP BY module, status
                ORDER BY module
                """;
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String module = rs.getString(1);
                String status = rs.getString(2);
                int cnt = rs.getInt(3);
                com.automation.dashboard.DbModuleStatEntry entry = byModule.computeIfAbsent(
                        module == null || module.isBlank() ? "Unknown" : module.trim(),
                        k -> {
                            com.automation.dashboard.DbModuleStatEntry e = new com.automation.dashboard.DbModuleStatEntry();
                            e.module = k;
                            return e;
                        });
                entry.total += cnt;
                if ("passed".equalsIgnoreCase(status)) {
                    entry.passed += cnt;
                } else if ("failed".equalsIgnoreCase(status)) {
                    entry.failed += cnt;
                } else if ("skipped".equalsIgnoreCase(status)) {
                    entry.skipped += cnt;
                }
            }
        } catch (SQLException e) {
            System.out.println("[DB-WARN] fetchModuleStatsFromTestResults skipped: " + e.getMessage());
        }
        return new java.util.ArrayList<>(byModule.values());
    }

    public static java.util.List<com.automation.dashboard.DbRecentFailEntry> fetchRecentFailedTests(int limit) {
        java.util.List<com.automation.dashboard.DbRecentFailEntry> list = new java.util.ArrayList<>();
        int safeLimit = Math.max(1, Math.min(limit, 50));
        String sql = """
                SELECT TOP %d case_id, module, error_message, run_date
                FROM failed_tests
                ORDER BY run_date DESC
                """.formatted(safeLimit);
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.automation.dashboard.DbRecentFailEntry row = new com.automation.dashboard.DbRecentFailEntry();
                    row.caseId = rs.getString(1);
                    row.module = rs.getString(2);
                    row.errorMessage = truncate(rs.getString(3), 300);
                    Timestamp ts = rs.getTimestamp(4);
                    row.runDate = ts == null ? "—" : ts.toLocalDateTime().toString().replace('T', ' ');
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            System.out.println("[DB-WARN] fetchRecentFailedTests skipped: " + e.getMessage());
        }
        return list;
    }

    private static int countSingle(String sql) {
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.out.println("[DB-WARN] count query skipped: " + e.getMessage());
            return 0;
        }
    }

    private static Connection openConnection() throws SQLException {
        loadDriver();
        String url = ConfigManager.get("db.sqlserver.url");
        if (ConfigManager.getBoolean("db.sqlserver.integratedSecurity", false)) {
            return DriverManager.getConnection(url);
        }
        String password = ConfigManager.get("db.sqlserver.password");
        return DriverManager.getConnection(
                url,
                ConfigManager.get("db.sqlserver.user"),
                password == null ? "" : password);
    }

    private static void loadDriver() throws SQLException {
        String driver = ConfigManager.get("db.sqlserver.driver");
        if (driver == null || driver.isBlank()) {
            return;
        }
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver not found: " + driver, e);
        }
    }

    private static void validateTable(String tableName) {
        if (tableName == null || !ALLOWED_TABLES.contains(tableName.trim().toLowerCase())) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }
    }

    private static void validateColumn(String column) {
        if (column == null || !column.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Invalid column name: " + column);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
