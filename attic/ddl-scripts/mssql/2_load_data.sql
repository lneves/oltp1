
USE master
GO

ALTER DATABASE tpce SET RECOVERY BULK_LOGGED
GO

ALTER DATABASE tpce SET TORN_PAGE_DETECTION OFF
GO

ALTER DATABASE tpce SET PAGE_VERIFY NONE
GO

USE tpce
GO

CHECKPOINT
GO

USE  tpce
GO

SET NOCOUNT ON
GO

PRINT 'bulk insert charge'
GO
BULK INSERT charge FROM '$(DATA_DIR)/Charge.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 5, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert commission_rate'
BULK INSERT commission_rate FROM '$(DATA_DIR)/CommissionRate.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 240, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert exchange'
GO
BULK INSERT exchange FROM '$(DATA_DIR)/Exchange.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 4, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert industry'
GO
BULK INSERT industry FROM '$(DATA_DIR)/Industry.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 102, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert sector'
GO
BULK INSERT sector FROM '$(DATA_DIR)/Sector.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 12, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert status_type'
GO
BULK INSERT status_type FROM '$(DATA_DIR)/Statustype.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 5, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert taxrate'
GO
BULK INSERT taxrate FROM '$(DATA_DIR)/TaxRate.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 320, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert trade_type'
GO
BULK INSERT trade_type FROM '$(DATA_DIR)/TradeType.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 5, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert zip_code'
GO
BULK INSERT zip_code FROM '$(DATA_DIR)/ZipCode.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 14741, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert account_permission'
GO
BULK INSERT account_permission FROM '$(DATA_DIR)/AccountPermission.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 195750, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert address'
GO
BULK INSERT address FROM '$(DATA_DIR)/Address.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 40504, TABLOCK)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert broker'
GO
BULK INSERT broker FROM '$(DATA_DIR)/Broker.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 270, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert company'
GO
BULK INSERT company FROM '$(DATA_DIR)/Company.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 13500, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert company_competitor'
GO
BULK INSERT company_competitor FROM '$(DATA_DIR)/CompanyCompetitor.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 40500, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert customer'
GO
BULK INSERT customer FROM '$(DATA_DIR)/Customer.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 27000, TABLOCK)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert customer_account'
GO
BULK INSERT customer_account FROM '$(DATA_DIR)/CustomerAccount.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 135000, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert customer_taxrate'
GO
BULK INSERT customer_taxrate FROM '$(DATA_DIR)/CustomerTaxrate.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 54000, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert daily_market'
GO
BULK INSERT daily_market FROM '$(DATA_DIR)/DailyMarket.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 24135975, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert financial'
GO
BULK INSERT financial FROM '$(DATA_DIR)/Financial.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 270000, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert last_trade'
GO
BULK INSERT last_trade FROM '$(DATA_DIR)/LastTrade.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 18495, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert news_item_temp'
GO
BULK INSERT news_item FROM '$(DATA_DIR)/NewsItem.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 27000, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert news_xref'
GO
BULK INSERT news_xref FROM '$(DATA_DIR)/NewsXRef.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 27000, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert security'
GO
BULK INSERT security FROM '$(DATA_DIR)/Security.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 18495, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert watch_item'
GO
BULK INSERT watch_item FROM '$(DATA_DIR)/WatchItem.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 2727000, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert watch_list'
GO
BULK INSERT watch_list FROM '$(DATA_DIR)/WatchList.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 27000, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert cash_transaction'
GO
BULK INSERT cash_transaction FROM '$(DATA_DIR)/CashTransaction.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 429701760, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert holding'
GO
BULK INSERT holding FROM '$(DATA_DIR)/Holding.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 25194240, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert holding_history'
GO
BULK INSERT holding_history FROM '$(DATA_DIR)/HoldingHistory.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 627056640, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert holding_summary'
GO
BULK INSERT holding_summary FROM '$(DATA_DIR)/HoldingSummary.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 1348650, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert settlement'
GO
BULK INSERT settlement FROM '$(DATA_DIR)/Settlement.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 466560000, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert trade'
GO
BULK INSERT trade FROM '$(DATA_DIR)/Trade.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 466560000, TABLOCK, KEEPNULLS, KEEPIDENTITY)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert trade_history'
GO
BULK INSERT trade_history FROM '$(DATA_DIR)/TradeHistory.txt' WITH ( FIELDTERMINATOR = '|', ROWTERMINATOR = '0x0A', ROWS_PER_BATCH = 1124409600, TABLOCK, KEEPNULLS)
GO
SELECT 'rows inserted:', @@rowcount
GO

PRINT 'bulk insert done!'
GO

USE master;
GO

-- EXEC sp_dboption tpce,'select into/bulkcopy',false
-- EXEC sp_dboption tpce,'trunc. log on chkpt.',false
-- EXEC sp_dboption tpce,'torn page detection',false
-- EXEC sp_dboption tpce,'Auto Update Statistics',false
-- GO

ALTER DATABASE tpce SET RECOVERY FULL
GO

ALTER DATABASE tpce SET PAGE_VERIFY CHECKSUM
GO

RECONFIGURE WITH OVERRIDE
GO

USE tpce
GO

CHECKPOINT
GO
