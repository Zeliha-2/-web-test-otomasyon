package com.automation.db;

import com.automation.config.ConfigManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class DbClient {
    private DbClient() {
    }

    public static boolean orderExists(String orderNo) {
        String sql = "SELECT COUNT(1) FROM orders WHERE order_no = ?";
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.get("db.url"),
                ConfigManager.get("db.user"),
                ConfigManager.get("db.password"));
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, orderNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("[DB-WARN] DB check skipped: " + e.getMessage());
        }
        return false;
    }
}
