package com.automation.tests;

import com.automation.config.ConfigManager;
import com.automation.dashboard.DashboardResultsListener;
import com.automation.db.DbClient;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/** SQL Server bağlantı smoke testi — SSMS TestDb kurulumunu doğrular. */
@Listeners(DashboardResultsListener.class)
public class SqlServerDbTest {

    @BeforeMethod(alwaysRun = true)
    public void guard() {
        if (!ConfigManager.getBoolean("run.db.ui.verify", false)) {
            throw new SkipException("run.db.ui.verify=false — SQL Server UI-DB doğrulaması kapalı");
        }
        if (!DbClient.isSqlServerAvailable()) {
            throw new SkipException(
                    "SQL Server erişilemedi. SSMS'te setup_testdb.sql çalıştırın ve "
                            + "config.properties içindeki db.sqlserver.* değerlerini kontrol edin.");
        }
    }

    @Test(description = "SQL Server TestDb ping + orders tablosu erişimi")
    public void sqlServerShouldBeReachable() {
        Assert.assertTrue(DbClient.isSqlServerAvailable(), "SQL Server should be reachable");
        Assert.assertEquals(DbClient.countOrdersByCaseId("__PING__"), 0);
        System.out.println("[SQL-SERVER-PASS] TestDb bağlantısı OK");
    }
}
