USE master;
GO


DROP DATABASE IF EXISTS tpce;
GO

CREATE DATABASE tpce; 
GO


IF EXISTS 
    (SELECT name FROM master.sys.server_principals WHERE name = 'tpce')
BEGIN
	DROP LOGIN tpce;
END
GO

CREATE LOGIN tpce  WITH PASSWORD = '$(BENCH_PASSWORD)';
GO

USE tpce;
GO

DROP USER IF EXISTS tpce
GO

CREATE USER tpce FOR LOGIN tpce;
GO

EXEC sp_addrolemember 'db_owner', 'tpce'
GO
