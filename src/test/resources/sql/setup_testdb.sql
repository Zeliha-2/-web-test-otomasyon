-- SSMS'te çalıştır: UI ↔ DB entegrasyonu için TestDb şeması
-- Dosya: src/test/resources/sql/setup_testdb.sql

IF DB_ID(N'TestDb') IS NULL
BEGIN
    CREATE DATABASE TestDb;
END
GO

USE TestDb;
GO

IF OBJECT_ID(N'dbo.orders', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.orders (
        id              INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        order_no        NVARCHAR(64)  NOT NULL,
        case_id         NVARCHAR(32)  NULL,
        product_name    NVARCHAR(200) NULL,
        customer_email  NVARCHAR(200) NULL,
        status          NVARCHAR(40)  NOT NULL CONSTRAINT DF_orders_status DEFAULT (N'CHECKOUT_STARTED'),
        ui_url          NVARCHAR(500) NULL,
        created_at      DATETIME2     NOT NULL CONSTRAINT DF_orders_created DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT UQ_orders_order_no UNIQUE (order_no)
    );
END
GO

IF OBJECT_ID(N'dbo.test_results', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.test_results (
        id              INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        test_name       NVARCHAR(200) NOT NULL,
        module          NVARCHAR(100) NULL,
        status          NVARCHAR(20)  NOT NULL,
        duration        NVARCHAR(20)  NULL,
        run_date        DATETIME2     NOT NULL CONSTRAINT DF_test_results_run DEFAULT (SYSUTCDATETIME()),
        error_message   NVARCHAR(MAX) NULL
    );
END
GO

IF OBJECT_ID(N'dbo.products', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.products (
        id              INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        name            NVARCHAR(200) NOT NULL,
        category        NVARCHAR(100) NULL,
        price           DECIMAL(10, 2) NOT NULL,
        stock           INT NOT NULL CONSTRAINT DF_products_stock DEFAULT (0)
    );
END
GO

IF OBJECT_ID(N'dbo.users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.users (
        id              INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        email           NVARCHAR(200) NOT NULL,
        password        NVARCHAR(200) NOT NULL,
        role            NVARCHAR(50)  NOT NULL,
        created_at      DATETIME2     NOT NULL CONSTRAINT DF_users_created DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT UQ_users_email UNIQUE (email)
    );
END
GO

IF OBJECT_ID(N'dbo.failed_tests', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.failed_tests (
        id              INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        case_id         NVARCHAR(50)  NULL,
        module          NVARCHAR(100) NULL,
        error_message   NVARCHAR(MAX) NULL,
        ai_analysis     NVARCHAR(MAX) NULL,
        run_date        DATETIME2     NOT NULL CONSTRAINT DF_failed_tests_run DEFAULT (SYSUTCDATETIME())
    );
END
GO

-- Java/Maven testleri için SQL login (Windows Auth yerine)
USE master;
GO
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'testauto')
BEGIN
    CREATE LOGIN testauto WITH PASSWORD = N'TestAuto123!', CHECK_POLICY = OFF;
END
GO

USE TestDb;
GO
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'testauto')
BEGIN
    CREATE USER testauto FOR LOGIN testauto;
    ALTER ROLE db_owner ADD MEMBER testauto;
END
GO

SELECT TOP 5 id, order_no, case_id, product_name, status, created_at
FROM dbo.orders
ORDER BY id DESC;
GO
