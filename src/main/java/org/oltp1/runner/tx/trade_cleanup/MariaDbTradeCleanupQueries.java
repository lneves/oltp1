package org.oltp1.runner.tx.trade_cleanup;

/**
 * Provides the MariaDB-specific SQL queries for the Trade-Cleanup transaction.
 */
public class MariaDbTradeCleanupQueries extends DefaultTradeCleanupQueries
{

	@Override
	public String insertTradeHistory1()
	{
		return """
				INSERT INTO trade_history (th_t_id, th_dts, th_st_id)
				SELECT
				    tr_t_id
				    , CURRENT_TIMESTAMP
				    , :st_id AS st_id
				FROM trade_request
				ON DUPLICATE KEY UPDATE
				    th_t_id = th_t_id;
				""";
	}

	@Override
	public String insertTradeHistory2()
	{
		return """
				INSERT INTO trade_history (th_t_id, th_dts, th_st_id)
				SELECT
				    t_id AS tr_t_id
				    , CURRENT_TIMESTAMP
				    , :st_canceled_id AS st_id
				FROM trade
				WHERE
				    t_id >= :start_trade_id
				    AND t_st_id = :st_submitted_id
				ON DUPLICATE KEY UPDATE
				    th_t_id = th_t_id;
					""";
	}

	@Override
	public String updateTrade1()
	{
		return """
				UPDATE trade
				SET
				    t_st_id = :st_canceled_id,
				    t_dts = CURRENT_TIMESTAMP
				WHERE t_id IN (
				    SELECT tr_t_id
				    FROM trade_request
				);
				""";
	}

	@Override
	public String updateTrade2()
	{
		return """
				UPDATE trade t1
				INNER JOIN trade t2 ON t1.t_id = t2.t_id
				SET
				    t1.t_st_id = :st_canceled_id
				    , t1.t_dts = CURRENT_TIMESTAMP
				WHERE
				    t2.t_id >= :start_trade_id
				    AND t2.t_st_id = :st_submitted_id;
				""";
	}
}
