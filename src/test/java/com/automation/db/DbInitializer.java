package com.automation.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQL Server TestDb şemasını oluşturur ve örnek veri ekler.
 */
public final class DbInitializer {
    private DbInitializer() {
    }

    public static void initialize() throws SQLException {
        if (!DbClient.isSqlServerAvailable()) {
            System.out.println("[DB-INIT-SKIP] SQL Server erişilemedi");
            return;
        }
        try (Connection conn = DbClient.openConnectionForInit();
             Statement st = conn.createStatement()) {
            createTables(st);
            seedData(st);
            System.out.println("[DB-INIT-OK] TestDb tabloları hazır");
        }
    }

    private static void createTables(Statement st) throws SQLException {
        st.execute("""
                IF OBJECT_ID(N'dbo.orders', N'U') IS NULL
                CREATE TABLE dbo.orders (
                    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                    order_no NVARCHAR(64) NOT NULL,
                    case_id NVARCHAR(32) NULL,
                    product_name NVARCHAR(200) NULL,
                    customer_email NVARCHAR(200) NULL,
                    status NVARCHAR(40) NOT NULL CONSTRAINT DF_orders_status DEFAULT (N'CHECKOUT_STARTED'),
                    ui_url NVARCHAR(500) NULL,
                    created_at DATETIME2 NOT NULL CONSTRAINT DF_orders_created DEFAULT (SYSUTCDATETIME()),
                    CONSTRAINT UQ_orders_order_no UNIQUE (order_no)
                )
                """);

        st.execute("""
                IF OBJECT_ID(N'dbo.test_results', N'U') IS NULL
                CREATE TABLE dbo.test_results (
                    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                    test_name NVARCHAR(200) NOT NULL,
                    module NVARCHAR(100) NULL,
                    status NVARCHAR(20) NOT NULL,
                    duration NVARCHAR(20) NULL,
                    run_date DATETIME2 NOT NULL CONSTRAINT DF_test_results_run DEFAULT (SYSUTCDATETIME()),
                    error_message NVARCHAR(MAX) NULL
                )
                """);

        st.execute("""
                IF OBJECT_ID(N'dbo.products', N'U') IS NULL
                CREATE TABLE dbo.products (
                    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                    name NVARCHAR(200) NOT NULL,
                    category NVARCHAR(100) NULL,
                    price DECIMAL(10, 2) NOT NULL,
                    stock INT NOT NULL CONSTRAINT DF_products_stock DEFAULT (0)
                )
                """);

        st.execute("""
                IF OBJECT_ID(N'dbo.users', N'U') IS NULL
                CREATE TABLE dbo.users (
                    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                    email NVARCHAR(200) NOT NULL,
                    password NVARCHAR(200) NOT NULL,
                    role NVARCHAR(50) NOT NULL,
                    created_at DATETIME2 NOT NULL CONSTRAINT DF_users_created DEFAULT (SYSUTCDATETIME()),
                    CONSTRAINT UQ_users_email UNIQUE (email)
                )
                """);

        st.execute("""
                IF OBJECT_ID(N'dbo.failed_tests', N'U') IS NULL
                CREATE TABLE dbo.failed_tests (
                    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                    case_id NVARCHAR(50) NULL,
                    module NVARCHAR(100) NULL,
                    error_message NVARCHAR(MAX) NULL,
                    ai_analysis NVARCHAR(MAX) NULL,
                    run_date DATETIME2 NOT NULL CONSTRAINT DF_failed_tests_run DEFAULT (SYSUTCDATETIME())
                )
                """);
    }

    private static void seedData(Statement st) throws SQLException {
        st.execute("""
                IF NOT EXISTS (SELECT 1 FROM dbo.products)
                BEGIN
                    SET IDENTITY_INSERT dbo.products ON;
                    INSERT INTO dbo.products (id, name, category, price, stock) VALUES
                    (1, N'iMac', N'Desktop', 1200.00, 15),
                    (2, N'MacBook', N'Laptops', 999.00, 20),
                    (3, N'HTC Touch HD', N'Phones', 249.00, 30),
                    (4, N'Canon EOS', N'Cameras', 899.00, 10),
                    (5, N'iPhone', N'Phones', 799.00, 25);
                    SET IDENTITY_INSERT dbo.products OFF;
                END
                """);

        st.execute("""
                IF NOT EXISTS (SELECT 1 FROM dbo.users)
                BEGIN
                    SET IDENTITY_INSERT dbo.users ON;
                    INSERT INTO dbo.users (id, email, password, role, created_at) VALUES
                    (1, N'admin@qatest.local', N'Admin123!', N'admin', SYSUTCDATETIME()),
                    (2, N'tester@qatest.local', N'Test1234!', N'tester', SYSUTCDATETIME()),
                    (3, N'guest@qatest.local', N'Guest123!', N'guest', SYSUTCDATETIME());
                    SET IDENTITY_INSERT dbo.users OFF;
                END
                """);

        st.execute("""
                IF NOT EXISTS (SELECT 1 FROM dbo.test_results)
                BEGIN
                    INSERT INTO dbo.test_results (test_name, module, status, duration, run_date, error_message) VALUES
                    (N'runLoginScenario [LGN-001]', N'Login', N'passed', N'1.2s', DATEADD(MINUTE, -30, SYSUTCDATETIME()), NULL),
                    (N'runApiScenario [API-GET-001]', N'Api', N'failed', N'2.8s', DATEADD(MINUTE, -15, SYSUTCDATETIME()), N'Response time exceeded 2000ms');
                END
                """);

        st.execute("""
                IF NOT EXISTS (SELECT 1 FROM dbo.failed_tests)
                BEGIN
                    INSERT INTO dbo.failed_tests (case_id, module, error_message, ai_analysis, run_date) VALUES
                    (N'CHK-SEC-01', N'Checkout', N'Guest checkout cart accessible without login',
                     N'[AI-SKIPPED] OWASP auth bypass demo failure', DATEADD(MINUTE, -10, SYSUTCDATETIME()));
                END
                """);
    }
}
