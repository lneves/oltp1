
INSERT INTO runtime_info (days_of_initial_trades, max_initial_t_id, end_of_initial_trades)
SELECT
	COUNT(DISTINCT cast(t_dts AS DATE)) AS days_of_initial_trades
	, MAX(t_id) AS max_initial_t_id
	, MAX(t_dts) AS end_of_initial_trades
FROM
	trade
WHERE t_dts < '2025-01-01';