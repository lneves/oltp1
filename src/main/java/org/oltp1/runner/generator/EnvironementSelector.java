package org.oltp1.runner.generator;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.oltp1.runner.db.SqlContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.data.Row;

public class EnvironementSelector
{
	private static final Logger log = LoggerFactory.getLogger(EnvironementSelector.class);

	private final int days_of_initial_trades;
	private final long max_initial_t_id;
	private final LocalDateTime end_of_initial_trades;

	public EnvironementSelector(SqlContext sqlCtx)
	{
		try (Connection con = sqlCtx.getSql2o().open())
		{
			Query tx = con.createQuery("""
					SELECT
					days_of_initial_trades
					, max_initial_t_id
					, end_of_initial_trades
					FROM runtime_info;
					""");

			Row row = tx.executeAndFetchTable().rows().getFirst();

			days_of_initial_trades = row.getInteger("days_of_initial_trades");
			max_initial_t_id = row.getLong("max_initial_t_id");
			end_of_initial_trades = row
					.getDate("end_of_initial_trades")
					.toInstant()
					.atZone(ZoneId.systemDefault())
					.toLocalDateTime();
			;

			log.info("Loaded environment information from the database");
			log.info(this.toString());
		}
	}

	public int getDaysOfInitialTrades()
	{
		return days_of_initial_trades - 1;
	}

	public long getMaxInitialTradeId()
	{
		return max_initial_t_id;
	}

	public LocalDateTime getEndOfInitialTrades()
	{
		return end_of_initial_trades;
	}

	@Override
	public String toString()
	{
		return String.format("EnvironementSelector [days_of_initial_trades=%s, max_initial_t_id=%s, end_of_initial_trades=%s]", days_of_initial_trades, max_initial_t_id, end_of_initial_trades);
	}

}