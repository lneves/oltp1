package org.oltp1.runner.tx.trade_cleanup;

/**
 * Provides the PostgreSQL-specific SQL queries for the Trade-Cleanup
 * transaction.
 */
public class PgSqlTradeCleanupQueries extends DefaultTradeCleanupQueries
{

	@Override
	public String insertTradeHistory1()
	{
		return """
				MERGE INTO trade_history AS th
				USING (
					SELECT tr_t_id, :st_id AS st_id
					FROM trade_request
				) AS src
				ON th.th_t_id = src.tr_t_id AND th.th_st_id = src.st_id
				WHEN MATCHED THEN
				    DO NOTHING
				WHEN NOT MATCHED THEN
				    INSERT (th_t_id, th_dts, th_st_id)
				    VALUES (src.tr_t_id, CURRENT_TIMESTAMP, src.st_id);
				               """;
	}

	@Override
	public String insertTradeHistory2()
	{
		return """
				MERGE INTO trade_history AS th
				USING (
				    SELECT
				        t_id AS tr_t_id,
				        :st_canceled_id AS st_id
				    FROM trade
				    WHERE
				        t_id >= :start_trade_id
				        AND t_st_id = :st_submitted_id
				) AS src
				ON th.th_t_id = src.tr_t_id AND th.th_st_id = src.st_id
				WHEN MATCHED THEN
				    DO NOTHING
				WHEN NOT MATCHED THEN
				    INSERT (th_t_id, th_dts, th_st_id)
				    VALUES (src.tr_t_id, CURRENT_TIMESTAMP, src.st_id);
								""";
	}
}
