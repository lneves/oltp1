package org.oltp1.runner.tx.broker_volume;

/**
 * Provides the Microsoft SQL Server-specific SQL queries for the Broker-Volume
 * transaction.
 */
public class MsSqlBrokerVolumeQueries implements BrokerVolumeQueries
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
					AND b_name IN (
								SELECT
									CAST(value AS varchar(49))
								FROM
									STRING_SPLIT(:broker_list,',')
								)
					AND sc_name = CAST(:sector_name AS varchar(30))
				GROUP BY
					b_name
				ORDER BY 2 DESC
								""";
	}
}
