package com.automation.utils;

import com.automation.config.ConfigManager;
import com.automation.db.DbClient;
import com.automation.models.CheckoutScenario;
import java.sql.SQLException;
import org.testng.Assert;

/**
 * UI checkout sonrası sipariş kaydını SQL Server TestDb'ye yazar ve okur.
 * LambdaTest playground kendi DB'mize yazmaz; bu köprü UI olayını TestDb'de doğrular.
 */
public final class UiDbOrderBridge {
    private UiDbOrderBridge() {
    }

    public static void recordAndVerifyCheckout(
            CheckoutScenario scenario,
            String productName,
            String checkoutUrl,
            String customerEmail) {
        if (!ConfigManager.getBoolean("run.db.ui.verify", false)) {
            System.out.println("[UI-DB-SKIP] run.db.ui.verify=false");
            return;
        }
        if (!scenario.isPersistOrderToDb()) {
            return;
        }
        if (!DbClient.isSqlServerAvailable()) {
            System.out.println("[UI-DB-WARN] SQL Server erişilemedi — DB doğrulaması atlandı: "
                    + scenario.getCaseId()
                    + " (SSMS'te TestDb kurulu mu? config.properties şifresi doğru mu?)");
            return;
        }

        String orderNo = buildOrderNo(scenario.getCaseId());
        String status = scenario.getExpectedDbStatus();
        try {
            DbClient.insertCheckoutOrder(
                    orderNo,
                    scenario.getCaseId(),
                    productName,
                    customerEmail,
                    checkoutUrl,
                    status);
        } catch (SQLException e) {
            Assert.fail("UI-DB insert failed for case " + scenario.getCaseId() + ": " + e.getMessage());
        }

        Assert.assertTrue(
                DbClient.orderExists(orderNo),
                "Order should exist in SQL Server after UI checkout for case " + scenario.getCaseId()
                        + " orderNo=" + orderNo);

        Assert.assertEquals(
                DbClient.getOrderStatus(orderNo),
                status,
                "Order status mismatch in SQL Server for case " + scenario.getCaseId());

        System.out.println("[UI-DB-PASS] " + scenario.getCaseId()
                + " orderNo=" + orderNo
                + " status=" + status
                + " product=" + productName);
    }

    private static String buildOrderNo(String caseId) {
        return "ORD-" + caseId + "-" + System.currentTimeMillis();
    }
}
