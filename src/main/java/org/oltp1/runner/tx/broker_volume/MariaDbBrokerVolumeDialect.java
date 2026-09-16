package org.oltp1.runner.tx.broker_volume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provides the Microsoft SQL Server-specific SQL queries for the Broker-Volume
 * transaction.
 */
public class MariaDbBrokerVolumeDialect implements BrokerVolumeDialect
{
	private final ObjectMapper json = new ObjectMapper();

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
				        SELECT value
				        FROM JSON_TABLE(
				            :broker_list
				            , '$[*]' COLUMNS (value VARCHAR(49) PATH '$')
				        ) AS jt
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
		try
		{
			return json.writeValueAsString(brokerList);
		}
		catch (JsonProcessingException e)
		{
			throw new RuntimeException("Failed to serialize broker list to JSON", e);
		}
	}
}
