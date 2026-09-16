package org.oltp1.runner.tx.broker_volume;

import org.apache.commons.lang3.StringUtils;

/**
 * Provides the PostgreSQL-specific SQL queries for the Broker-Volume
 * transaction.
 */
public class PgSqlBrokerVolumeDialect implements BrokerVolumeDialect
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
					JOIN trade_request ON tr_b_id = b_id
					JOIN security ON tr_s_symb = s_symb
					JOIN company ON s_co_id = co_id
					JOIN industry ON co_in_id = in_id
					JOIN sector ON sc_id = in_sc_id
				WHERE
					b_name IN (
						SELECT bn::varchar
						FROM string_to_table(:broker_list, ',') AS bn
					)
					AND sc_name = :sector_name
				GROUP BY
					b_name
				ORDER BY 2 DESC;
								""";
	}

	@Override
	public String buildBrokerList(String[] brokerList)
	{
		return StringUtils.join(brokerList, ",");
	}
}
