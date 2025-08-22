package org.oltp1.runner.tx.broker_volume;

/**
 * Provides the PostgreSQL-specific SQL queries for the Broker-Volume
 * transaction.
 */
public class PgSqlBrokerVolumeQueries implements BrokerVolumeQueries
{
	@Override
	public String getVolume()
	{
		return """
				SELECT
					b_name AS broker_name
					, SUM(tr_qty * tr_bid_price) AS volume
				FROM
					broker
					, trade_request
					, security
					, company
					, industry
					, sector
				WHERE
					tr_b_id = b_id
					AND tr_s_symb = s_symb
					AND s_co_id = co_id
					AND co_in_id = in_id
					AND sc_id = in_sc_id
				 	AND b_name = ANY(:broker_list)
					AND sc_name = :sector_name
				GROUP BY
					b_name
				ORDER BY 2 DESC;
								""";
	}
}
