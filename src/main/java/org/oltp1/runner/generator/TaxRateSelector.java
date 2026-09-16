package org.oltp1.runner.generator;

import org.oltp1.runner.db.SqlContext;
import org.sql2o.Connection;

public class TaxRateSelector
{
	private final String[] taxRateIds; // loaded once at startup

	public TaxRateSelector(SqlContext sqlCtx)
	{
		try (Connection con = sqlCtx.getSql2o().open())
		{
			taxRateIds = con
					.createQuery("SELECT DISTINCT tx_id FROM taxrate ORDER BY tx_id")
					.executeScalarList(String.class)
					.toArray(new String[0]);
		}

		if (taxRateIds.length == 0)
			throw new IllegalStateException("taxrate table is empty");

	}

	public String randomTaxRateId()
	{
		return taxRateIds[ThreadLocalCRandom.get().rndIntRange(0, taxRateIds.length - 1)];
	}
}
