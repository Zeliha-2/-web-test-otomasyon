package com.automation.dashboard;

import com.automation.config.ConfigManager;
import com.automation.db.DbClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** SQL Server test_results / failed_tests / orders tablolarından dashboard snapshot üretir. */
public final class DbDashboardExporter {
    private static final Path SNAPSHOT_JSON = Path.of("target", "test-history", "db-snapshot.json");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private DbDashboardExporter() {
    }

    public static void exportIfAvailable() {
        DbDashboardSnapshot snapshot = buildSnapshot();
        if (!snapshot.sqlAvailable) {
            System.out.println("[DB-DASHBOARD-SKIP] SQL Server snapshot atlandı");
            return;
        }
        try {
            Files.createDirectories(SNAPSHOT_JSON.getParent());
            MAPPER.writeValue(SNAPSHOT_JSON.toFile(), snapshot);
            System.out.println("[DB-DASHBOARD-OK] db-snapshot.json yazıldı — "
                    + snapshot.moduleStats.size() + " modül, "
                    + snapshot.testResultsCount + " test kaydı");
        } catch (IOException e) {
            System.err.println("[DB-DASHBOARD-WARN] db-snapshot.json yazılamadı: " + e.getMessage());
        }
    }

    public static DbDashboardSnapshot buildSnapshot() {
        DbDashboardSnapshot snapshot = new DbDashboardSnapshot();
        snapshot.exportedAt = LocalDateTime.now().format(FMT);
        if (!ConfigManager.getBoolean("run.db.ui.verify", false) || !DbClient.isSqlServerAvailable()) {
            snapshot.sqlAvailable = false;
            return snapshot;
        }
        snapshot.sqlAvailable = true;
        snapshot.testResultsCount = DbClient.countAllTestResults();
        snapshot.failedTestsCount = DbClient.countAllFailedTests();
        snapshot.ordersCount = DbClient.countAllOrders();
        snapshot.moduleStats = DbClient.fetchModuleStatsFromTestResults();
        snapshot.recentFailed = DbClient.fetchRecentFailedTests(12);
        return snapshot;
    }
}
