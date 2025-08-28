package org.oltp1.runner.tx.trade_cleanup;

/**
 * Provides the database agnostic SQL queries for the Trade-Cleanup transaction.
 */
public abstract class DefaultTradeCleanupQueries implements TradeCleanupQueries
{

	@Override
	public String deleteTradeRequest()
	{
		return "DELETE FROM trade_request;";
	}

	@Override
	public String updateTrade1()
	{
		return """
				WITH tr_req AS (

				SELECT tr_t_id FROM trade_request
				)

				UPDATE trade
				SET
					t_st_id = :st_canceled_id
					, t_dts = CURRENT_TIMESTAMP
				FROM tr_req
				WHERE t_id = tr_req.tr_t_id;
				""";
	}

	@Override
	public String updateTrade2()
	{
		return """
				WITH tr_req AS (

				SELECT
					t_id AS trade_id
				FROM
					trade
				WHERE
					t_id >= :start_trade_id
					AND t_st_id = :st_submitted_id
				)

				UPDATE trade
				SET
					t_st_id = :st_canceled_id
					, t_dts = CURRENT_TIMESTAMP
				FROM tr_req
				WHERE t_id = tr_req.trade_id;
				""";
	}
}
