package com.automation.tests;



import com.automation.config.ConfigManager;

import com.automation.dashboard.DashboardResultsListener;

import com.automation.db.DbClient;

import com.automation.db.DbInitializer;

import com.automation.db.H2DbClient;

import com.automation.models.DbScenario;

import com.automation.utils.JsonDataLoader;

import java.lang.reflect.Method;

import java.sql.Connection;

import java.sql.SQLException;

import org.testng.Assert;

import org.testng.SkipException;

import org.testng.annotations.AfterClass;

import org.testng.annotations.BeforeClass;

import org.testng.annotations.BeforeMethod;

import org.testng.annotations.DataProvider;

import org.testng.annotations.Listeners;

import org.testng.annotations.Test;



@Listeners(DashboardResultsListener.class)

public class DbTest {



    private Connection connection;

    private boolean h2Available;

    private String h2SkipReason = "";

    private boolean sqlServerAvailable;

    private String sqlSkipReason = "";



    @BeforeClass(alwaysRun = true)

    public void connectDatabases() {

        connectH2();

        connectSqlServer();

    }



    private void connectH2() {

        if (!ConfigManager.getBoolean("run.db.layer", false)) {

            return;

        }

        try {

            connection = H2DbClient.openConnection();

            connection.setAutoCommit(true);

            h2Available = connection.isValid(3);

            if (!h2Available) {

                h2SkipReason = "H2 connection isValid() returned false";

            }

        } catch (SQLException e) {

            h2Available = false;

            h2SkipReason = e.getMessage();

            System.out.println("[DB-SKIP] H2 bağlantısı kurulamadı: " + h2SkipReason);

        }

    }



    private void connectSqlServer() {

        if (!ConfigManager.getBoolean("run.db.ui.verify", false)) {

            return;

        }

        sqlServerAvailable = DbClient.isSqlServerAvailable();

        if (!sqlServerAvailable) {

            sqlSkipReason = "SQL Server ping failed";

            System.out.println("[DB-SKIP] SQL Server erişilemedi");

        }

    }



    @AfterClass(alwaysRun = true)

    public void closeH2() {

        if (connection != null) {

            try {

                connection.close();

            } catch (SQLException e) {

                System.out.println("[DB-WARN] Connection close failed: " + e.getMessage());

            }

        }

    }



    @BeforeMethod(alwaysRun = true)

    public void guardDbLayer(Method method) {

        if ("runSqlServerDbScenario".equals(method.getName())) {

            guardSqlServerLayer(method);

            return;

        }

        guardH2Layer(method);

    }



    private void guardH2Layer(Method method) {

        if (!ConfigManager.getBoolean("run.db.layer", false)) {

            throw new SkipException("run.db.layer=false — H2 DB katmanı devre dışı");

        }

        if (!h2Available || connection == null) {

            throw new SkipException("H2 bağlantısı yok — test atlandı: " + h2SkipReason);

        }

        try {

            if (!connection.isValid(2)) {

                throw new SkipException("H2 bağlantısı kesildi — test atlandı");

            }

        } catch (SQLException e) {

            throw new SkipException("H2 bağlantı kontrolü başarısız: " + e.getMessage());

        }

        System.out.println("[DB-TEST-START] " + method.getName());

    }



    private void guardSqlServerLayer(Method method) {

        if (!ConfigManager.getBoolean("run.db.ui.verify", false)) {

            throw new SkipException("run.db.ui.verify=false — SQL Server DB katmanı devre dışı");

        }

        if (!sqlServerAvailable) {

            throw new SkipException("SQL Server bağlantısı yok — test atlandı: " + sqlSkipReason);

        }

        System.out.println("[DB-SS-TEST-START] " + method.getName());

    }



    @DataProvider(name = "dbScenarios")

    public Object[][] dbScenarios() {

        DbScenario[] scenarios = JsonDataLoader.load("testdata/db-scenarios.json", DbScenario[].class);

        Object[][] data = new Object[scenarios.length][1];

        for (int i = 0; i < scenarios.length; i++) {

            data[i][0] = scenarios[i];

        }

        return data;

    }



    @DataProvider(name = "sqlServerDbScenarios")

    public Object[][] sqlServerDbScenarios() {

        DbScenario[] scenarios = JsonDataLoader.load("testdata/db-sqlserver-scenarios.json", DbScenario[].class);

        Object[][] data = new Object[scenarios.length][1];

        for (int i = 0; i < scenarios.length; i++) {

            data[i][0] = scenarios[i];

        }

        return data;

    }



    @Test(

            dataProvider = "dbScenarios",

            description = "Data-driven H2 in-memory CRUD scenarios (ISTQB)")

    public void runDbScenario(DbScenario scenario) throws SQLException {

        String op = scenario.getOperation() == null ? "" : scenario.getOperation().trim().toUpperCase();



        switch (op) {

            case "CREATE_TABLE" -> {

                H2DbClient.createProductsTable(connection);

                Assert.assertTrue(H2DbClient.ping(), "H2 should remain reachable after CREATE TABLE");

            }

            case "INSERT" -> runInsert(scenario);

            case "SELECT_COUNT" -> {

                Integer filterId = scenario.getRecordId();

                int actual = filterId == null

                        ? H2DbClient.countProducts(connection)

                        : H2DbClient.countProducts(connection, filterId);

                Assert.assertEquals(

                        actual,

                        scenario.getExpectedCount().intValue(),

                        "Row count mismatch for case " + scenario.getCaseId());

            }

            case "SELECT_VALUE" -> {

                String actual = H2DbClient.readColumn(

                        connection,

                        scenario.getRecordId(),

                        scenario.getExpectedColumn());

                Assert.assertEquals(

                        actual,

                        scenario.getExpectedValue(),

                        "Column value mismatch for case " + scenario.getCaseId());

            }

            case "SELECT_NULL" -> {

                String actual = H2DbClient.readColumn(

                        connection,

                        scenario.getRecordId(),

                        scenario.getExpectedColumn());

                Assert.assertNull(actual, "Expected no row for case " + scenario.getCaseId());

            }

            case "SELECT_SUM" -> {

                int actual = H2DbClient.sumQuantity(connection);

                Assert.assertEquals(

                        actual,

                        scenario.getExpectedCount().intValue(),

                        "Quantity sum mismatch for case " + scenario.getCaseId());

            }

            case "UPDATE" -> {

                int updated = H2DbClient.updateProduct(connection, scenario);

                Assert.assertEquals(

                        updated,

                        scenario.getExpectedCount().intValue(),

                        "Updated row count mismatch for case " + scenario.getCaseId());

            }

            case "DELETE" -> {

                int deleted = H2DbClient.deleteProduct(connection, scenario.getRecordId());

                Assert.assertEquals(

                        deleted,

                        scenario.getExpectedCount().intValue(),

                        "Deleted row count mismatch for case " + scenario.getCaseId());

            }

            default -> Assert.fail("Unknown DB operation '" + op + "' for case " + scenario.getCaseId());

        }



        System.out.println("[DB-PASS] " + scenario.getCaseId()

                + " | " + op

                + " | " + scenario.getTechnique());

    }



    @Test(

            dataProvider = "sqlServerDbScenarios",

            description = "Data-driven SQL Server TestDb table scenarios (ISTQB)")

    public void runSqlServerDbScenario(DbScenario scenario) throws SQLException {

        String op = scenario.getOperation() == null ? "" : scenario.getOperation().trim().toUpperCase();

        String table = scenario.getTableName();



        switch (op) {

            case "SS_INIT" -> DbInitializer.initialize();

            case "SS_COUNT" -> {

                int actual = DbClient.countTable(table);

                Assert.assertEquals(

                        actual,

                        scenario.getExpectedCount().intValue(),

                        "Table count mismatch for " + table + " case " + scenario.getCaseId());

            }

            case "SS_COUNT_MIN" -> {

                int actual = DbClient.countTable(table);

                Assert.assertTrue(

                        actual >= scenario.getExpectedCount().intValue(),

                        "Table count below minimum for " + table + " case " + scenario.getCaseId()

                                + " actual=" + actual);

            }

            case "SS_SELECT_VALUE" -> {

                String actual = DbClient.readColumnById(

                        table,

                        scenario.getRecordId(),

                        scenario.getExpectedColumn());

                Assert.assertEquals(

                        actual,

                        scenario.getExpectedValue(),

                        "Column mismatch for " + table + " case " + scenario.getCaseId());

            }

            case "SS_SELECT_NULL" -> {

                String actual = DbClient.readColumnById(

                        table,

                        scenario.getRecordId(),

                        scenario.getExpectedColumn());

                Assert.assertNull(actual, "Expected no row for " + table + " case " + scenario.getCaseId());

            }

            default -> Assert.fail("Unknown SQL Server operation '" + op + "' for case " + scenario.getCaseId());

        }



        System.out.println("[DB-SS-PASS] " + scenario.getCaseId()

                + " | " + op

                + " | " + scenario.getTechnique());

    }



    private void runInsert(DbScenario scenario) throws SQLException {

        boolean expectError = Boolean.TRUE.equals(scenario.getExpectSqlError());

        try {

            int inserted = H2DbClient.insertProduct(connection, scenario);

            if (expectError) {

                Assert.fail("Expected SQL error for duplicate/invalid insert in case " + scenario.getCaseId());

            }

            Assert.assertEquals(inserted, 1, "Insert should affect 1 row for case " + scenario.getCaseId());

        } catch (SQLException e) {

            if (!expectError) {

                throw e;

            }

            System.out.println("[DB-EXPECTED-ERROR] " + scenario.getCaseId() + ": " + e.getMessage());

        }

    }

}


