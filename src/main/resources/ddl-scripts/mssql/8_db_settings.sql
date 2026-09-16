USE master;
GO

ALTER DATABASE tpce SET ALLOW_SNAPSHOT_ISOLATION ON;
GO

ALTER DATABASE tpce SET READ_COMMITTED_SNAPSHOT ON;
GO


EXEC sp_configure 'show advanced options', 1;
GO
RECONFIGURE;
GO


USE tpce;
GO

DECLARE @next_tid bigint;

SELECT @next_tid = (MAX(t_id)) FROM trade;

DBCC CHECKIDENT ('dbo.Trade', RESEED, @next_tid); 
GO

TRUNCATE TABLE runtime_info;
GO

INSERT INTO runtime_info (days_of_initial_trades, max_initial_t_id, end_of_initial_trades)
SELECT
	COUNT(DISTINCT cast(t_dts AS DATE)) AS days_of_initial_trades
	, MAX(t_id) AS max_initial_t_id
	, MAX(t_dts) AS end_of_initial_trades
FROM
	trade;
GO

CREATE TYPE dbo.bigint_list_type AS TABLE
(
    ivalue BIGINT NOT NULL PRIMARY KEY
);
GO

CREATE TYPE dbo.string_list_type AS TABLE
(
    ivalue VARCHAR(49) NOT NULL PRIMARY KEY
);
GO
